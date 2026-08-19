from datetime import datetime
from typing import Dict, List, Optional

import numpy as np
import pandas as pd
import torch


def _cosine(a: np.ndarray, b: np.ndarray) -> float:
    denom = np.linalg.norm(a) * np.linalg.norm(b)
    if denom < 1e-9:
        return 0.0
    return float(np.dot(a, b) / denom)


class ExhibitionRecommender:
    """展览推荐引擎"""

    def __init__(self, config, model, data_processor, device: str = 'cpu'):
        self.config = config
        self.model = model
        self.data_processor = data_processor
        self.device = torch.device(device)
        self.model.to(self.device)
        self.model.eval()
        self.exhibition_embeddings_cache: Optional[Dict[int, np.ndarray]] = None

    # ------------------------------------------------------------------ #
    def recommend(self, user_id: int) -> List[Dict]:
        """主推荐函数：输入用户 id，返回推荐结果列表（按得分降序）"""
        user_info = self.data_processor.process_user_features(user_id)

        # 冷启动处理（用户不存在或无行为）
        if user_info['is_cold_start']:
            return self._cold_start_recommend(user_info.get('city'))

        active_exhibitions = self._get_active_exhibitions()
        if not active_exhibitions:
            return []

        # 构建/刷新展览向量缓存
        if self.exhibition_embeddings_cache is None:
            self._update_exhibition_cache(active_exhibitions)

        user_vector = self._compute_user_vector(user_info)
        scores = self._calculate_scores(user_vector, active_exhibitions, user_info)
        return self._diversify_rerank(scores, k=self.config.top_k)

    # ------------------------------------------------------------------ #
    def _compute_user_vector(self, user_info: Dict) -> np.ndarray:
        type_pref, gallery_pref = self.data_processor.get_user_model_features(user_info)
        user_idx = user_info['user_idx']
        with torch.no_grad():
            vec = self.model.forward_user(
                torch.tensor([user_idx], dtype=torch.long, device=self.device),
                torch.tensor(np.asarray(type_pref)[None], dtype=torch.float32, device=self.device),
                torch.tensor(np.asarray(gallery_pref)[None], dtype=torch.float32, device=self.device),
            )
        return vec[0].cpu().numpy()

    def _calculate_scores(self, user_vector: np.ndarray, exhibitions: List[Dict],
                          user_info: Dict) -> List[Dict]:
        """计算综合得分：相关性 + 同城加权 + 闭幕紧迫度"""
        scores = []
        now = datetime.now()
        interacted = (user_info['features']['fav_exhibition_ids']
                      | user_info['features']['browse_history'])
        user_city = user_info.get('city')

        for exhibition in exhibitions:
            eid = exhibition['id']
            # 已交互过的展览不再重复推荐
            if eid in interacted:
                continue
            exhibition_vector = self.exhibition_embeddings_cache.get(eid)
            if exhibition_vector is None:
                continue

            # 相关性：余弦相似度 [-1,1] 归一化到 [0,1]
            sim = _cosine(user_vector, exhibition_vector)
            relevance = (sim + 1.0) / 2.0

            # 闭幕紧迫度：剩余天数越少越紧迫（原实现方向相反，已修正）
            end_time = pd.to_datetime(exhibition['end_time']).to_pydatetime()
            days_remaining = (end_time - now).days
            if days_remaining <= 0:   # 已过期
                continue
            urgency = 1.0 - min(1.0, days_remaining / 90.0)

            final_score = (self.config.relevance_weight * relevance
                           + self.config.urgency_weight * urgency)

            # 同城加权
            city_match = (user_city is not None and exhibition.get('city') == user_city)
            if city_match:
                final_score *= self.config.city_boost

            scores.append({
                'exhibition_id': eid,
                'score': round(float(final_score), 6),
                'city_match': bool(city_match),
                'relevance': round(relevance, 6),
                'days_remaining': int(days_remaining),
                'is_hot': False,
            })

        return sorted(scores, key=lambda x: x['score'], reverse=True)

    def _diversify_rerank(self, scores: List[Dict], k: int = 4) -> List[Dict]:
        """MMR 多样性重排序，避免推荐过于相似"""
        if len(scores) <= k:
            return scores

        selected = [scores[0]]
        candidates = scores[1:]

        while len(selected) < k and candidates:
            best_candidate, best_mmr = None, -float('inf')
            for candidate in candidates:
                cand_emb = self.exhibition_embeddings_cache[candidate['exhibition_id']]
                max_sim = max(
                    _cosine(cand_emb, self.exhibition_embeddings_cache[s['exhibition_id']])
                    for s in selected
                )
                mmr_score = candidate['score'] - self.config.diversity_factor * max_sim
                if mmr_score > best_mmr:
                    best_candidate, best_mmr = candidate, mmr_score
            selected.append(best_candidate)
            candidates.remove(best_candidate)

        return selected

    # ------------------------------------------------------------------ #
    def _cold_start_recommend(self, city: Optional[str] = None) -> List[Dict]:
        """冷启动推荐：热门展览 + 同城优先"""
        active_exhibitions = self._get_active_exhibitions()
        if not active_exhibitions:
            return []

        hot_exhibitions = []
        for exhibition in active_exhibitions:
            view_count = float(exhibition.get('view_count', 0) or 0)
            fav_count = float(exhibition.get('fav_count', 0) or 0)
            interactions = view_count + fav_count
            hot_score = view_count + 2.0 * fav_count \
                + self._time_freshness(exhibition['end_time']) * 100.0

            city_match = (city is not None and exhibition.get('city') == city)
            if city_match:
                hot_score *= 1.2

            hot_exhibitions.append({
                'exhibition_id': exhibition['id'],
                'score': round(float(hot_score), 4),
                'city_match': bool(city_match),
                'is_hot': True,
                '_interactions': interactions,
            })

        # 达到互动阈值的优先；全部不达标则回退全量
        qualified = [h for h in hot_exhibitions
                     if h['_interactions'] >= self.config.min_interactions]
        pool = qualified if qualified else hot_exhibitions
        pool.sort(key=lambda x: x['score'], reverse=True)
        pool = pool[:20]

        results = self._diversify_hot_recommendations(pool, k=self.config.top_k)
        for item in results:
            item.pop('_interactions', None)
        return results

    def _diversify_hot_recommendations(self, hot_items: List[Dict], k: int = 4) -> List[Dict]:
        """确保热门推荐也有一定多样性"""
        if not hot_items:
            return []

        selected = [hot_items[0]]
        type_set = {self._get_exhibition_type(hot_items[0]['exhibition_id'])}

        for item in hot_items[1:]:
            if len(selected) >= k:
                break
            item_type = self._get_exhibition_type(item['exhibition_id'])
            if item_type not in type_set or len(selected) < k / 2:
                selected.append(item)
                type_set.add(item_type)

        # 不够 k 个时补充剩余高分项
        if len(selected) < k:
            for item in hot_items:
                if item not in selected:
                    selected.append(item)
                if len(selected) >= k:
                    break

        return selected[:k]

    # ------------------------------------------------------------------ #
    def _time_freshness(self, end_time) -> float:
        """时间新鲜度：窗口期内线性衰减"""
        days_left = (pd.to_datetime(end_time).to_pydatetime() - datetime.now()).days
        if days_left <= 0:
            return 0.0
        return max(0.0, 1.0 - days_left / max(self.config.hot_days, 1))

    def _get_active_exhibitions(self) -> List[Dict]:
        """获取当前有效展览"""
        now = datetime.now()
        active = []
        for ex in self.data_processor.exhibitions.to_dict('records'):
            end_time = pd.to_datetime(ex['end_time']).to_pydatetime()
            if end_time > now and ex['id'] in self.data_processor.exhibition_id_to_idx:
                active.append(ex)
        return active

    def _update_exhibition_cache(self, exhibitions: List[Dict]):
        """批量计算展览向量缓存"""
        if not exhibitions:
            return
        # 批量提取特征
        feats = []
        for ex in exhibitions:
            f = self.data_processor.process_exhibition_features(ex)
            feats.append(f)
        # 构造批量张量
        batch = {
            'idx': torch.tensor([f['exhibition_idx'] for f in feats], dtype=torch.long),
            'type': torch.tensor([f['type_encoded'] for f in feats], dtype=torch.long),
            'gallery_type': torch.tensor([f['gallery_type'] for f in feats], dtype=torch.long),
            'city': torch.tensor([f['city_encoded'] for f in feats], dtype=torch.long),
            'artist_vector': torch.tensor(np.stack([f['artist_vector'] for f in feats]), dtype=torch.float32),
            'text_embedding': torch.tensor(np.stack([f['text_embedding'] for f in feats]), dtype=torch.float32),
            'time_features': torch.tensor(np.stack([f['time_features'] for f in feats]), dtype=torch.float32),
        }
        batch = {k: v.to(self.device) for k, v in batch.items()}
        embeddings = self.model.get_exhibition_embeddings(batch)
        embeddings = embeddings.cpu().numpy()
        self.exhibition_embeddings_cache = {
            ex['id']: embeddings[i] for i, ex in enumerate(exhibitions)
        }

    def invalidate_cache(self):
        self.exhibition_embeddings_cache = None

    def _get_exhibition_type(self, exhibition_id: int):
        rows = self.data_processor.exhibitions[
            self.data_processor.exhibitions['id'] == exhibition_id
        ]
        return rows['type'].iloc[0] if len(rows) > 0 else None