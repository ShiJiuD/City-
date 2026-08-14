import json
from datetime import datetime
from typing import Dict, List, Optional, Union

import jieba
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import HashingVectorizer
from sklearn.preprocessing import LabelEncoder

UNKNOWN_INDEX = 0  # 未知类别的编码


class DataProcessor:
    def __init__(self, config):
        self.config = config
        self.label_encoders: Dict[str, LabelEncoder] = {}
        self.user_id_to_idx: Dict = {}          # 原始 user_id -> 模型索引
        self.exhibition_id_to_idx: Dict = {}    # 原始 exhibition_id -> 模型索引
        self.users: Optional[pd.DataFrame] = None
        self.exhibitions: Optional[pd.DataFrame] = None
        self.galleries: Optional[pd.DataFrame] = None
        self.user_behaviors: Optional[pd.DataFrame] = None
        self._artist_class_set = set()

        self._text_vectorizer = HashingVectorizer(
            n_features=config.text_embedding_dim,
            alternate_sign=False,
            norm='l2',
            tokenizer=lambda text: list(jieba.cut(text)),
            token_pattern=None,
        )

    # 数据加载
    def load_data(self):
        dd = self.config.data_dir
        self.users = pd.read_csv(f'{dd}/users.csv')
        self.exhibitions = pd.read_csv(
            f'{dd}/exhibitions.csv',
            parse_dates=['start_time', 'end_time'],
        )
        self.galleries = pd.read_csv(f'{dd}/galleries.csv')
        self.user_behaviors = pd.read_csv(
            f'{dd}/user_behaviors.csv',
            parse_dates=['timestamp'],
        )

    # 编码器
    def fit_encoders(self):
        """在全量数据上统一 fit"""
        # ---- 展览类型 ----
        le = LabelEncoder()
        le.fit(pd.concat([pd.Series(['__unknown__']), self.exhibitions['type'].astype(str)]).unique())
        self.label_encoders['exhibition_type'] = le

        # ---- 美术馆类型（处理空表或缺失列） ----
        le = LabelEncoder()
        if len(self.galleries) > 0 and 'type' in self.galleries.columns:
            gallery_types = self.galleries['type'].astype(str)
        else:
            gallery_types = pd.Series()
        le.fit(pd.concat([pd.Series(['__unknown__']), gallery_types]).unique())
        self.label_encoders['gallery_type'] = le

        # ---- 城市（合并用户和展览的城市） ----
        le = LabelEncoder()
        all_cities = pd.concat([self.users['city'], self.exhibitions['city']])
        le.fit(pd.concat([pd.Series(['__unknown__']), all_cities.dropna().astype(str)]).unique())
        self.label_encoders['city'] = le

        # ---- 艺术家 ----
        all_artists = set()
        for value in self.exhibitions['artists']:
            all_artists.update(str(a) for a in self._parse_list(value))
        le = LabelEncoder()
        le.fit(pd.concat([pd.Series(['__unknown__']), pd.Series(sorted(all_artists))]).unique())
        self.label_encoders['artists'] = le
        self._artist_class_set = set(le.classes_)

        # ---- 原始 id -> 模型索引 ----
        self.user_id_to_idx = {uid: i for i, uid in enumerate(self.users['id'].tolist())}
        self.exhibition_id_to_idx = {eid: i for i, eid in enumerate(self.exhibitions['id'].tolist())}

    def _safe_transform(self, feature_name: str, value) -> int:
        """未见过的值为 UNKNOWN_INDEX"""
        le = self.label_encoders.get(feature_name)
        if le is None:
            return UNKNOWN_INDEX
        try:
            return int(le.transform([str(value)])[0])
        except ValueError:
            return UNKNOWN_INDEX

    def _multi_hot_encode(self, items: List, feature_name: str) -> np.ndarray:
        """多热编码"""
        le = self.label_encoders.get(feature_name)
        if le is None:
            return np.zeros(1, dtype=np.float32)
        vec = np.zeros(len(le.classes_), dtype=np.float32)
        known = [str(it) for it in items if str(it) in self._artist_class_set]
        if known:
            vec[le.transform(known)] = 1.0
        return vec

    def export_state(self) -> Dict:
        return {
            'label_encoders': self.label_encoders,
            'user_id_to_idx': self.user_id_to_idx,
            'exhibition_id_to_idx': self.exhibition_id_to_idx,
        }

    def import_state(self, state: Dict):
        self.label_encoders = state['label_encoders']
        self.user_id_to_idx = state['user_id_to_idx']
        self.exhibition_id_to_idx = state['exhibition_id_to_idx']
        artists = self.label_encoders.get('artists')
        self._artist_class_set = set(artists.classes_) if artists is not None else set()

    # 展览特征
    def process_exhibition_features(self, exhibition=None) -> Union[pd.DataFrame, Dict]:

        if exhibition is None:
            records = [
                self._build_single_exhibition_features(row)
                for _, row in self.exhibitions.iterrows()
            ]
            return pd.DataFrame(records)
        return self._build_single_exhibition_features(exhibition)

    def _build_single_exhibition_features(self, row) -> Dict:
        get = row.get if hasattr(row, 'get') else lambda k, d=None: row[k]
        start_time = pd.to_datetime(row['start_time'])
        end_time = pd.to_datetime(row['end_time'])

        gallery_rows = self.galleries[self.galleries['id'] == row['gallery_id']]
        gallery_type = gallery_rows.iloc[0]['type'] if len(gallery_rows) > 0 else '__unknown__'

        return {
            'exhibition_id': row['id'],
            'exhibition_idx': self.exhibition_id_to_idx.get(row['id'], UNKNOWN_INDEX),
            'type_encoded': self._safe_transform('exhibition_type', get('type', '__unknown__')),
            'artist_vector': self._multi_hot_encode(self._parse_list(get('artists', '[]')), 'artists'),
            'text_embedding': self._text_to_embedding(get('description', '')),
            'gallery_type': self._safe_transform('gallery_type', gallery_type),
            'city_encoded': self._safe_transform('city', get('city', '')),
            'city': get('city', ''),
            'is_active': bool(end_time > pd.Timestamp(datetime.now())),
            'time_features': self._extract_time_features(start_time, end_time),
            'start_time': start_time,
            'end_time': end_time,
            'view_count': float(get('view_count', 0) or 0),
            'fav_count': float(get('fav_count', 0) or 0),
        }

    def _text_to_embedding(self, text) -> np.ndarray:
        """文本转向量：哈希 """
        dim = self.config.text_embedding_dim
        if text is None or not str(text).strip():
            return np.zeros(dim, dtype=np.float32)
        vec = self._text_vectorizer.transform([str(text)]).toarray()[0]
        return vec.astype(np.float32)

    def _extract_time_features(self, start_time, end_time) -> np.ndarray:
        """提取 5 维数值时间特征"""
        now = datetime.now()
        start_time = pd.to_datetime(start_time).to_pydatetime()
        end_time = pd.to_datetime(end_time).to_pydatetime()
        days_to_start = (start_time - now).days
        duration = (end_time - start_time).days
        feats = np.array([
            days_to_start / 90.0,                              # 距离开展
            duration / 180.0,                                  # 展期长度
            1.0 if start_time.weekday() >= 5 else 0.0,         # 是否周末开展
            start_time.month / 12.0,                           # 月份
            ((start_time.month - 1) // 3 + 1) / 4.0,           # 季度
        ], dtype=np.float32)
        return np.clip(feats, -3.0, 3.0)

    # 用户特征
    def process_user_features(self, user_id) -> Dict:
        """处理用户特征，处理冷启动问题"""
        user_rows = self.users[self.users['id'] == user_id]
        if len(user_rows) == 0:
            return {'is_cold_start': True, 'city': None, 'user_idx': None, 'features': None}

        user_data = user_rows.iloc[0]
        behaviors = self.user_behaviors[self.user_behaviors['user_id'] == user_id]
        if len(behaviors) == 0:
            return {
                'is_cold_start': True,
                'city': user_data.get('city'),
                'user_idx': self.user_id_to_idx.get(user_id),
                'features': None,
            }

        hist_types = self._parse_list(user_data.get('history_exhibition_types', '[]'))
        fav_ids = self._parse_list(user_data.get('fav_exhibition_ids', '[]'))
        hist_gallery_types = self._parse_list(user_data.get('history_gallery_types', '[]'))
        browse_ids = behaviors[behaviors['action'] == 'browse']['exhibition_id'].tolist()
        fav_ids_from_behavior = behaviors[behaviors['action'] == 'fav']['exhibition_id'].tolist()
        fav_ids = list(set(fav_ids) | set(fav_ids_from_behavior))

        features = {
            'history_exhibition_types': hist_types,
            'fav_exhibition_ids': set(fav_ids),           # 用于推理时过滤已交互展览
            'browse_history': set(browse_ids),
            'type_preference': self._build_type_preference_vector(hist_types, fav_ids, browse_ids),
            'gallery_preference': self._build_gallery_preference_vector(hist_gallery_types),
        }

        # 类型分布（保留给可解释性/后续规则使用）
        type_counts = {}
        for t in hist_types:
            type_counts[str(t)] = type_counts.get(str(t), 0) + 1
        features['type_distribution'] = type_counts

        return {
            'is_cold_start': False,
            'city': user_data.get('city'),
            'user_idx': self.user_id_to_idx.get(user_id),
            'features': features,
        }

    def _build_type_preference_vector(self, hist_types, fav_ids, browse_ids) -> np.ndarray:
        """按行为权重构建归一化的展览类型偏好向量"""
        le = self.label_encoders.get('exhibition_type')
        if le is None:
            return np.zeros(1, dtype=np.float32)
        vec = np.zeros(len(le.classes_), dtype=np.float32)

        for t in hist_types:
            vec[self._safe_transform('exhibition_type', t)] += 1.0

        ex_type_map = dict(zip(self.exhibitions['id'], self.exhibitions['type'].astype(str)))
        for eid in fav_ids:
            t = ex_type_map.get(eid)
            if t is not None:
                vec[self._safe_transform('exhibition_type', t)] += self.config.fav_weight
        for eid in browse_ids:
            t = ex_type_map.get(eid)
            if t is not None:
                vec[self._safe_transform('exhibition_type', t)] += self.config.browse_weight

        total = vec.sum()
        if total > 0:
            vec /= total
        return vec

    def _build_gallery_preference_vector(self, hist_gallery_types) -> np.ndarray:
        """按历史观展记录构建归一化的美术馆类型偏好向量"""
        le = self.label_encoders.get('gallery_type')
        if le is None:
            return np.zeros(1, dtype=np.float32)
        vec = np.zeros(len(le.classes_), dtype=np.float32)
        for t in hist_gallery_types:
            vec[self._safe_transform('gallery_type', t)] += self.config.gallery_weight
        total = vec.sum()
        if total > 0:
            vec /= total
        return vec

    def get_user_model_features(self, user_info: Dict):
        """返回用户塔输入 ，冷启动用户返回全零"""
        n_types = len(self.label_encoders['exhibition_type'].classes_)
        n_gallery = len(self.label_encoders['gallery_type'].classes_)
        if user_info.get('features') is None:
            return (np.zeros(n_types, dtype=np.float32),
                    np.zeros(n_gallery, dtype=np.float32))
        return (user_info['features']['type_preference'],
                user_info['features']['gallery_preference'])

    # 工具方法
    @staticmethod
    def _parse_list(value) -> List:
        """解析列表字段（JSON 字符串 -> list），非法/空值返回 []"""
        if value is None:
            return []
        if isinstance(value, list):
            return value
        if isinstance(value, float) and np.isnan(value):
            return []
        if isinstance(value, str):
            value = value.strip()
            if not value:
                return []
            try:
                parsed = json.loads(value)
                return parsed if isinstance(parsed, list) else []
            except (json.JSONDecodeError, ValueError):
                return []
        return []