import json
import warnings
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Any

import numpy as np
import pandas as pd
from sklearn.decomposition import TruncatedSVD
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler, LabelEncoder

warnings.filterwarnings('ignore')


class FeatureEngineer:
    """特征工程类"""

    def __init__(self, config):
        self.config = config
        self.scalers = {}
        self.encoders = {}
        self.text_vectorizer = None
        self.text_svd = None

    @property
    def text_embedding_dim(self):
        return getattr(self.config, 'text_embedding_dim', 128)

    # 用户特征
    def build_user_features(self, user_data: Dict, user_behaviors: pd.DataFrame) -> Dict:
        """构建用户特征，返回结构化特征字典"""
        features = {}
        features.update(self._build_user_statistical_features(user_behaviors))
        features.update(self._build_user_preference_features(user_data, user_behaviors))
        features.update(self._build_user_temporal_features(user_behaviors))
        features.update(self._build_user_cross_features(features))
        return features

    def _build_user_statistical_features(self, behaviors: pd.DataFrame) -> Dict:
        """构建用户统计特征"""
        if len(behaviors) == 0:
            return {
                'total_interactions': 0,
                'browse_count': 0,
                'fav_count': 0,
                'unique_exhibitions': 0,
                'avg_interaction_per_day': 0,
                'interaction_diversity': 0,
            }

        browse_behaviors = behaviors[behaviors['action'] == 'browse']
        fav_behaviors = behaviors[behaviors['action'] == 'fav']

        features = {
            'total_interactions': len(behaviors),
            'browse_count': len(browse_behaviors),
            'fav_count': len(fav_behaviors),
            'unique_exhibitions': behaviors['exhibition_id'].nunique(),
            'fav_ratio': len(fav_behaviors) / max(len(behaviors), 1),
        }

        if 'timestamp' in behaviors.columns:
            time_range = (behaviors['timestamp'].max() -
                          behaviors['timestamp'].min()).days + 1
            features['active_days'] = time_range
            features['avg_interaction_per_day'] = len(behaviors) / max(time_range, 1)

            recent_days = 7
            recent_behaviors = behaviors[
                behaviors['timestamp'] >= datetime.now() - timedelta(days=recent_days)
                ]
            features['recent_interactions'] = len(recent_behaviors)
            features['recent_activity_score'] = len(recent_behaviors) / recent_days

        return features

    def _build_user_preference_features(self, user_data: Dict,
                                        behaviors: pd.DataFrame) -> Dict:
        """构建用户偏好特征"""
        features = {}

        if 'history_exhibition_types' in user_data:
            type_list = user_data['history_exhibition_types']
            if isinstance(type_list, str):
                type_list = json.loads(type_list)

            type_counts = {}
            for t in type_list:
                type_counts[t] = type_counts.get(t, 0) + 1

            if type_counts:   # 修复：空列表时不再对空字典调用 max()
                total = sum(type_counts.values())
                features['exhibition_type_distribution'] = {
                    k: v / total for k, v in type_counts.items()
                }
                features['primary_type'] = max(type_counts, key=type_counts.get)
                features['type_diversity'] = len(type_counts)

        if 'history_gallery_types' in user_data:
            gallery_types = user_data['history_gallery_types']
            if isinstance(gallery_types, str):
                gallery_types = json.loads(gallery_types)

            gallery_counts = {}
            for g in gallery_types:
                gallery_counts[g] = gallery_counts.get(g, 0) + 1

            if gallery_counts:   # 修复：同上
                features['gallery_type_distribution'] = gallery_counts
                features['primary_gallery_type'] = max(gallery_counts, key=gallery_counts.get)

        if 'fav_exhibition_ids' in user_data:
            fav_ids = user_data['fav_exhibition_ids']
            if isinstance(fav_ids, str):
                fav_ids = json.loads(fav_ids)

            features['fav_exhibition_ids'] = fav_ids
            features['has_favorites'] = len(fav_ids) > 0
            if len(fav_ids) > 0:
                features['fav_count'] = len(fav_ids)

        return features

    def _build_user_temporal_features(self, behaviors: pd.DataFrame) -> Dict:
        """构建用户时序行为特征"""
        if len(behaviors) == 0 or 'timestamp' not in behaviors.columns:
            return {}

        features = {}
        behaviors = behaviors.sort_values('timestamp').copy()  # 修复：copy 避免链式赋值警告

        time_diffs = behaviors['timestamp'].diff().dt.total_seconds() / 3600
        features['avg_interaction_interval'] = time_diffs.mean()
        features['std_interaction_interval'] = time_diffs.std()

        behaviors['hour'] = behaviors['timestamp'].dt.hour
        hour_counts = behaviors['hour'].value_counts()
        features['peak_hour'] = hour_counts.index[0] if len(hour_counts) > 0 else 12

        behaviors['is_weekend'] = behaviors['timestamp'].dt.dayofweek >= 5
        weekend_ratio = behaviors['is_weekend'].mean()
        features['weekend_preference'] = 'weekend' if weekend_ratio > 0.5 else 'weekday'
        features['weekend_ratio'] = weekend_ratio

        return features

    def _build_user_cross_features(self, base_features: Dict) -> Dict:
        """构建用户交叉特征"""
        features = {}

        if 'total_interactions' in base_features:
            interactions = base_features['total_interactions']
            if interactions == 0:
                features['user_maturity'] = 'cold_start'
            elif interactions < 10:
                features['user_maturity'] = 'new'
            elif interactions < 50:
                features['user_maturity'] = 'active'
            else:
                features['user_maturity'] = 'expert'

        if 'fav_count' in base_features and 'browse_count' in base_features:
            total = base_features['fav_count'] + base_features['browse_count']
            features['fav_propensity'] = base_features['fav_count'] / total if total > 0 else 0

        return features

    # 展览特征
    def fit_label_encoders(self, exhibitions_df: pd.DataFrame):
        """在全量展览上统一 fit 类别编码器（build_exhibition_features 会自动调用）"""
        for name, col in [('exhibition_type', 'type'), ('city', 'city'),
                          ('gallery', 'gallery_id')]:
            if col in exhibitions_df.columns:
                le = LabelEncoder()
                le.fit(pd.concat([pd.Series(['__unknown__']),
                                  exhibitions_df[col].astype(str)]).unique())
                self.encoders[name] = le
        if 'gallery_type' in exhibitions_df.columns:
            le = LabelEncoder()
            le.fit(pd.concat([pd.Series(['__unknown__']),
                              exhibitions_df['gallery_type'].astype(str)]).unique())
            self.encoders['gallery_type'] = le

    def _label_encode(self, feature_name: str, value: Any) -> int:
        """标签编码，未 fit 或未见过的值为 0"""
        encoder = self.encoders.get(feature_name)
        if encoder is None:
            return 0
        try:
            return int(encoder.transform([str(value)])[0])
        except ValueError:
            return 0

    def _fit_text_pipeline(self, texts: List[str]):
        """对全量语料一次性 fit TF-IDF 向量化器和 SVD 降维器"""
        valid_texts = [t for t in texts if t.strip()]
        if not valid_texts:
            return
        self.text_vectorizer = TfidfVectorizer(max_features=5000, ngram_range=(1, 2))
        tfidf_matrix = self.text_vectorizer.fit_transform(valid_texts)
        n_components = min(self.text_embedding_dim,
                           max(1, tfidf_matrix.shape[0] - 1),
                           max(1, tfidf_matrix.shape[1] - 1))
        self.text_svd = TruncatedSVD(n_components=n_components, random_state=42)
        self.text_svd.fit(tfidf_matrix)

    def build_exhibition_features(self, exhibition_data: pd.DataFrame) -> pd.DataFrame:
        """构建展览特征矩阵，返回处理后的特征 DataFrame"""
        self.fit_label_encoders(exhibition_data)

        corpus = []
        for _, exhibition in exhibition_data.iterrows():
            text_fields = []
            for field in ['title', 'description', 'artist_name', 'tags']:
                if field in exhibition and exhibition[field] is not None \
                        and not (isinstance(exhibition[field], float)
                                 and np.isnan(exhibition[field])):
                    text_fields.append(str(exhibition[field]))
            corpus.append(' '.join(text_fields))
        self._fit_text_pipeline(corpus)

        feature_list = []
        for (idx, exhibition), combined_text in zip(exhibition_data.iterrows(), corpus):
            features = {}
            features.update(self._build_exhibition_basic_features(exhibition))
            features.update(self._build_exhibition_text_features(exhibition, combined_text))
            features.update(self._build_exhibition_time_features(exhibition))
            features.update(self._build_exhibition_gallery_features(exhibition))
            features.update(self._build_exhibition_popularity_features(exhibition))
            features.update(self._build_exhibition_artist_features(exhibition))
            feature_list.append(features)

        features_df = pd.DataFrame(feature_list)
        features_df = self._scale_and_encode_features(features_df)
        return features_df

    def _build_exhibition_basic_features(self, exhibition: pd.Series) -> Dict:
        """展览基础特征"""
        features = {
            'exhibition_id': exhibition['id'],
            'exhibition_type': exhibition.get('type', 0),
            'exhibition_type_encoded': self._label_encode('exhibition_type', exhibition.get('type', 0)),
            'city': exhibition.get('city', ''),
            'city_encoded': self._label_encode('city', exhibition.get('city', '')),
        }

        if 'price' in exhibition:
            features['price'] = exhibition['price']
            features['is_free'] = 1 if exhibition['price'] == 0 else 0
            features['price_level'] = self._categorize_price(exhibition['price'])

        return features

    def _build_exhibition_text_features(self, exhibition: pd.Series, combined_text: str) -> Dict:
        """展览文本特征（TF-IDF + SVD 降维）"""
        features = {}
        dim = self.text_embedding_dim

        if not combined_text.strip() or self.text_vectorizer is None:
            features['text_vector'] = np.zeros(dim)
            return features

        try:
            text_tfidf = self.text_vectorizer.transform([combined_text])
            text_vector = self.text_svd.transform(text_tfidf)[0]
            # SVD 输出维度可能小于配置维度（样本少时），统一补齐
            if len(text_vector) < dim:
                text_vector = np.pad(text_vector, (0, dim - len(text_vector)))
            features['text_vector'] = text_vector

            features['title_length'] = len(str(exhibition.get('title', '') or ''))
            features['description_length'] = len(str(exhibition.get('description', '') or ''))
            features['has_description'] = 1 if exhibition.get('description') else 0
            features['has_tags'] = 1 if exhibition.get('tags') else 0
        except Exception:
            features['text_vector'] = np.zeros(dim)

        return features

    def _build_exhibition_time_features(self, exhibition: pd.Series) -> Dict:
        """展览时间特征工程"""
        features = {}
        now = datetime.now()
        start_time = pd.to_datetime(exhibition['start_time'])
        end_time = pd.to_datetime(exhibition['end_time'])

        features['days_to_start'] = (start_time - now).days
        features['days_to_end'] = (end_time - now).days
        features['is_ongoing'] = 1 if features['days_to_start'] <= 0 else 0
        features['is_expired'] = 1 if features['days_to_end'] < 0 else 0

        features['duration_days'] = (end_time - start_time).days
        features['is_long_term'] = 1 if features['duration_days'] > 90 else 0
        features['is_short_term'] = 1 if features['duration_days'] < 7 else 0

        features['start_month'] = start_time.month
        features['start_season'] = (start_time.month - 1) // 3 + 1
        features['start_dayofweek'] = start_time.dayofweek
        features['start_is_weekend'] = 1 if start_time.dayofweek >= 5 else 0

        if features['days_to_end'] > 0:
            features['urgency_score'] = 1 / (1 + np.exp(features['days_to_end'] / 7))
        else:
            features['urgency_score'] = 0

        features['month_sin'] = np.sin(2 * np.pi * start_time.month / 12)
        features['month_cos'] = np.cos(2 * np.pi * start_time.month / 12)
        features['dayofweek_sin'] = np.sin(2 * np.pi * start_time.dayofweek / 7)
        features['dayofweek_cos'] = np.cos(2 * np.pi * start_time.dayofweek / 7)

        return features

    def _build_exhibition_gallery_features(self, exhibition: pd.Series) -> Dict:
        """美术馆相关特征"""
        features = {}

        if 'gallery_id' in exhibition:
            features['gallery_id'] = exhibition['gallery_id']
            features['gallery_id_encoded'] = self._label_encode('gallery', exhibition['gallery_id'])

        if 'gallery_type' in exhibition:
            features['gallery_type'] = exhibition['gallery_type']
            features['gallery_type_encoded'] = self._label_encode('gallery_type', exhibition['gallery_type'])

        if 'gallery_rating' in exhibition:
            features['gallery_rating'] = exhibition['gallery_rating']
            features['is_high_rated_gallery'] = 1 if exhibition['gallery_rating'] >= 4.5 else 0

        return features

    def _build_exhibition_popularity_features(self, exhibition: pd.Series) -> Dict:
        """展览热度特征"""
        features = {}
        features['view_count'] = exhibition.get('view_count', 0)
        features['fav_count'] = exhibition.get('fav_count', 0)
        features['share_count'] = exhibition.get('share_count', 0)

        features['popularity_score'] = (
                features['view_count'] * 1.0 +
                features['fav_count'] * 2.0 +
                features['share_count'] * 3.0
        )

        pop_score = features['popularity_score']
        if pop_score < 10:
            features['popularity_level'] = 'low'
        elif pop_score < 100:
            features['popularity_level'] = 'medium'
        elif pop_score < 500:
            features['popularity_level'] = 'high'
        else:
            features['popularity_level'] = 'hot'

        features['popularity_log'] = np.log1p(features['popularity_score'])

        if 'days_to_start' in features:
            time_weight = np.exp(-abs(features['days_to_start']) / 30)
            features['time_weighted_popularity'] = features['popularity_log'] * time_weight

        return features

    def _build_exhibition_artist_features(self, exhibition: pd.Series) -> Dict:
        """艺术家特征"""
        features = {}

        if 'artist_ids' in exhibition:
            artist_ids = exhibition['artist_ids']
            if isinstance(artist_ids, str):
                artist_ids = json.loads(artist_ids)

            features['artist_count'] = len(artist_ids)
            features['is_solo_exhibition'] = 1 if len(artist_ids) == 1 else 0
            features['is_group_exhibition'] = 1 if len(artist_ids) > 1 else 0

            if 'artist_rating' in exhibition:
                features['artist_rating'] = exhibition['artist_rating']
                features['is_famous_artist'] = 1 if exhibition['artist_rating'] >= 4.0 else 0

        return features

    def _categorize_price(self, price: float) -> str:
        """价格分档"""
        if price == 0:
            return 'free'
        elif price < 50:
            return 'low'
        elif price < 150:
            return 'medium'
        elif price < 300:
            return 'high'
        else:
            return 'premium'

    def _scale_and_encode_features(self, features_df: pd.DataFrame) -> pd.DataFrame:
        """特征缩放和编码"""
        df = features_df.copy()

        numeric_columns = df.select_dtypes(include=[np.number]).columns
        for col in numeric_columns:
            if col in ('exhibition_id', 'gallery_id'):
                continue
            if col not in self.scalers:
                self.scalers[col] = StandardScaler()
                df[col] = self.scalers[col].fit_transform(
                    df[col].values.reshape(-1, 1)).ravel()
            else:
                df[col] = self.scalers[col].transform(
                    df[col].values.reshape(-1, 1)).ravel()

        categorical_columns = ['popularity_level']
        for col in categorical_columns:
            if col in df.columns:
                dummies = pd.get_dummies(df[col], prefix=col)
                df = pd.concat([df, dummies], axis=1)
                df.drop(col, axis=1, inplace=True)

        return df

    # 交叉特征与向量聚合
    def build_cross_features(self, user_features: Dict,
                             exhibition_features: pd.Series) -> Dict:
        """构建用户-展览交叉特征"""
        cross_features = {}

        if ('exhibition_type_distribution' in user_features and
                'exhibition_type' in exhibition_features):
            user_type_dist = user_features['exhibition_type_distribution']
            exhibition_type = exhibition_features['exhibition_type']
            cross_features['type_match_score'] = user_type_dist.get(exhibition_type, 0)

        if ('gallery_type_distribution' in user_features and
                'gallery_type' in exhibition_features):
            user_gallery_dist = user_features['gallery_type_distribution']
            gallery_type = exhibition_features['gallery_type']
            cross_features['gallery_match_score'] = user_gallery_dist.get(gallery_type, 0)

        if 'user_maturity' in user_features:
            maturity_score = {
                'cold_start': 0.5,
                'new': 0.3,
                'active': 0.8,
                'expert': 0.2,
            }.get(user_features['user_maturity'], 0.5)
            cross_features['novelty_preference'] = maturity_score

        return cross_features

    def aggregate_user_embedding(self, user_features: Dict,
                                 weight_dict: Dict = None) -> np.ndarray:
        """将用户特征聚合为固定长度的向量"""
        if weight_dict is None:
            weight_dict = {
                'statistical': 0.2,
                'preference': 0.4,
                'temporal': 0.2,
                'cross': 0.2,
            }

        embedding_parts = []

        stat_features = []
        stat_keys = ['total_interactions', 'browse_count', 'fav_count',
                     'unique_exhibitions', 'fav_ratio', 'active_days']
        for key in stat_keys:
            stat_features.append(user_features.get(key, 0))
        embedding_parts.append(np.array(stat_features) * weight_dict['statistical'])

        pref_features = []
        if 'exhibition_type_distribution' in user_features:
            type_dist = user_features['exhibition_type_distribution']
            sorted_types = sorted(type_dist.items(), key=lambda x: x[1], reverse=True)
            pref_features = [sorted_types[i][1] for i in range(min(5, len(sorted_types)))]
        # 修复：统一补齐到 5 维，避免不同用户向量长度不一致
        pref_features.extend([0.0] * (5 - len(pref_features)))
        pref_features.append(1 if user_features.get('has_favorites', False) else 0)
        embedding_parts.append(np.array(pref_features) * weight_dict['preference'])

        temporal_features = []
        temporal_keys = ['avg_interaction_per_day', 'recent_interactions',
                         'recent_activity_score', 'weekend_ratio']
        for key in temporal_keys:
            temporal_features.append(user_features.get(key, 0))
        embedding_parts.append(np.array(temporal_features) * weight_dict['temporal'])

        user_embedding = np.concatenate(embedding_parts)
        norm = np.linalg.norm(user_embedding)
        if norm > 0:
            user_embedding = user_embedding / norm

        return user_embedding

    def prepare_training_data(self, user_data_list: List[Dict],
                              exhibition_df: pd.DataFrame,
                              behaviors_df: pd.DataFrame) -> Tuple:
        """准备训练数据（特征工程完整流程）"""
        print("开始特征工程...")

        user_features_list = []
        for user_data in user_data_list:
            user_id = user_data['id']
            user_behaviors = behaviors_df[behaviors_df['user_id'] == user_id]
            user_features = self.build_user_features(user_data, user_behaviors)
            user_features['user_id'] = user_id
            user_features_list.append(user_features)

        exhibition_features_df = self.build_exhibition_features(exhibition_df)

        training_samples = []
        for behavior in behaviors_df.to_dict('records'):
            user_id = behavior['user_id']
            exhibition_id = behavior['exhibition_id']

            user_feat = next(
                (uf for uf in user_features_list if uf['user_id'] == user_id),
                None
            )
            exhibition_feat = exhibition_features_df[
                exhibition_features_df['exhibition_id'] == exhibition_id
                ]

            if user_feat is not None and not exhibition_feat.empty:
                cross_feat = self.build_cross_features(user_feat, exhibition_feat.iloc[0])
                user_emb = self.aggregate_user_embedding(user_feat)

                training_samples.append({
                    'user_id': user_id,
                    'exhibition_id': exhibition_id,
                    'user_embedding': user_emb,
                    'cross_features': cross_feat,
                    'label': 1 if behavior['action'] == 'fav' else 0.5,
                    'weight': self.config.fav_weight if behavior['action'] == 'fav'
                    else self.config.browse_weight,
                })

        print(f"特征工程完成，生成 {len(training_samples)} 个训练样本")
        return training_samples, user_features_list, exhibition_features_df


# 使用示例
if __name__ == "__main__":
    from config import Config

    config = Config()
    fe = FeatureEngineer(config)

    sample_user = {
        'id': 1,
        'history_exhibition_types': [1, 0, 2, 1],
        'fav_exhibition_ids': [101, 102],
        'history_gallery_types': [1, 1, 2],
    }

    sample_behaviors = pd.DataFrame({
        'user_id': [1, 1, 1],
        'exhibition_id': [101, 102, 103],
        'action': ['browse', 'fav', 'browse'],
        'timestamp': pd.date_range('2024-01-01', periods=3),
    })

    user_features = fe.build_user_features(sample_user, sample_behaviors)
    print("用户特征构建完成:")
    for key, value in user_features.items():
        print(f"  {key}: {value}")

    # 空偏好的边界场景验证
    empty_user = {'id': 2, 'history_exhibition_types': [],
                  'history_gallery_types': [], 'fav_exhibition_ids': []}
    empty_features = fe.build_user_features(empty_user, sample_behaviors.iloc[0:0])
    print("空偏好用户特征构建完成（无崩溃）")