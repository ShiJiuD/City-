import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import Dict, Optional


def build_mlp(input_dim: int, hidden_dims, output_dim: int, dropout_rate: float) -> nn.Sequential:
    """构建 MLP：input -> [Linear+ReLU+Dropout] x len(hidden_dims) -> Linear(output)"""
    layers = []
    prev = input_dim
    for h in hidden_dims:
        layers += [nn.Linear(prev, h), nn.ReLU(), nn.Dropout(dropout_rate)]
        prev = h
    layers.append(nn.Linear(prev, output_dim))
    return nn.Sequential(*layers)


class BPRLoss(nn.Module):
    """BPR 损失，支持每个正样本对应多个负样本以及样本权重"""

    def forward(self, pos_scores: torch.Tensor, neg_scores: torch.Tensor,
                sample_weight: Optional[torch.Tensor] = None) -> torch.Tensor:
        """
        pos_scores:    [B]
        neg_scores:    [B] 或 [B, num_neg]
        sample_weight: [B]，可选（收藏行为权重高于浏览）
        """
        if neg_scores.dim() > 1:
            diff = pos_scores.unsqueeze(-1) - neg_scores      # [B, num_neg]
        else:
            diff = pos_scores - neg_scores
        per_sample = -F.logsigmoid(diff).mean(dim=-1)         # [B]
        if sample_weight is not None:
            return (per_sample * sample_weight).sum() / sample_weight.sum().clamp(min=1e-8)
        return per_sample.mean()


class ExhibitionRecommendationModel(nn.Module):
    """双塔模型：用户塔 + 展览塔"""

    def __init__(self, config, num_users: int, num_exhibitions: int,
                 num_exhibition_types: int, num_gallery_types: int,
                 num_cities: int, num_artists: int):
        super().__init__()
        self.config = config
        dim = config.embedding_dim

        #  用户塔
        self.user_embedding = nn.Embedding(num_users, dim)
        # 用户类型/美术馆偏好：归一化分布向量 -> 投影到 embedding 空间
        self.user_type_projection = nn.Linear(num_exhibition_types, dim)
        self.user_gallery_projection = nn.Linear(num_gallery_types, dim)
        self.user_mlp = build_mlp(dim * 3, config.mlp_hidden_dims, dim, config.dropout_rate)

        #  展览塔
        self.exhibition_embedding = nn.Embedding(num_exhibitions, dim)
        self.exhibition_type_embedding = nn.Embedding(num_exhibition_types, dim)
        self.gallery_type_embedding = nn.Embedding(num_gallery_types, dim)
        self.city_embedding = nn.Embedding(num_cities, dim)
        self.artist_projection = nn.Linear(num_artists, dim)      # 艺术家 multi-hot -> 向量
        self.text_projection = nn.Linear(config.text_embedding_dim, dim)
        self.time_mlp = nn.Sequential(
            nn.Linear(5, 32),   # 5 个时间特征（见 data_processor._extract_time_features）
            nn.ReLU(),
            nn.Linear(32, dim),
        )
        # 7 路特征融合：id / 类型 / 美术馆类型 / 城市 / 艺术家 / 文本 / 时间
        self.exhibition_mlp = build_mlp(dim * 7, config.mlp_hidden_dims, dim, config.dropout_rate)

        self.bpr_loss = BPRLoss()

    def forward_user(self, user_idx: torch.Tensor, type_pref: torch.Tensor,
                     gallery_pref: torch.Tensor) -> torch.Tensor:
        """用户塔前向传播（全部为批量张量）

        user_idx:     [B] long，用户索引（0~N-1，由 data_processor 映射）
        type_pref:    [B, num_exhibition_types] float，归一化类型偏好分布
        gallery_pref: [B, num_gallery_types] float，归一化美术馆类型偏好分布
        返回:         [B, embedding_dim]，已 L2 归一化
        """
        user_emb = self.user_embedding(user_idx)                    # [B, dim]
        type_emb = self.user_type_projection(type_pref)             # [B, dim]
        gallery_emb = self.user_gallery_projection(gallery_pref)    # [B, dim]
        user_vector = torch.cat([user_emb, type_emb, gallery_emb], dim=-1)
        return F.normalize(self.user_mlp(user_vector), dim=-1)

    def forward_exhibition(self, features: Dict[str, torch.Tensor]) -> torch.Tensor:
        """展览塔前向传播（批量）

        features 键与形状（支持 [B, ...] 或 [B, num_neg, ...] 两种前导形状）：
            idx:            long    展览索引
            type:           long    展览类型编码
            gallery_type:   long    美术馆类型编码
            city:           long    城市编码
            artist_vector:  float   艺术家 multi-hot
            text_embedding: float   文本特征
            time_features:  float   [..., 5] 时间特征
        返回: 与前导形状对应的 L2 归一化向量
        """
        exhibition_emb = self.exhibition_embedding(features['idx'])
        type_emb = self.exhibition_type_embedding(features['type'])
        gallery_emb = self.gallery_type_embedding(features['gallery_type'])
        city_emb = self.city_embedding(features['city'])
        artist_emb = self.artist_projection(features['artist_vector'])
        text_emb = self.text_projection(features['text_embedding'])
        time_emb = self.time_mlp(features['time_features'])

        exhibition_vector = torch.cat(
            [exhibition_emb, type_emb, gallery_emb, city_emb,
             artist_emb, text_emb, time_emb],
            dim=-1,
        )
        return F.normalize(self.exhibition_mlp(exhibition_vector), dim=-1)

    def forward(self, user_idx: torch.Tensor, type_pref: torch.Tensor,
                gallery_pref: torch.Tensor,
                pos_features: Dict[str, torch.Tensor],
                neg_features: Dict[str, torch.Tensor],
                sample_weight: Optional[torch.Tensor] = None) -> torch.Tensor:
        """训练用前向传播，返回加权 BPR 损失

        pos_features: 各张量形状 [B, ...]
        neg_features: 各张量形状 [B, num_neg, ...]
        """
        user_vec = self.forward_user(user_idx, type_pref, gallery_pref)  # [B, dim]
        pos_vec = self.forward_exhibition(pos_features)                  # [B, dim]
        neg_vec = self.forward_exhibition(neg_features)                  # [B, num_neg, dim]

        pos_score = (user_vec * pos_vec).sum(dim=-1)                    # [B]
        neg_score = (user_vec.unsqueeze(1) * neg_vec).sum(dim=-1)       # [B, num_neg]
        return self.bpr_loss(pos_score, neg_score, sample_weight)


    @torch.no_grad()
    def get_user_embedding(self, user_idx: torch.Tensor, type_pref: torch.Tensor,
                           gallery_pref: torch.Tensor) -> torch.Tensor:
        """获取用户向量（推理用），输入形状 [1, ...]"""
        return self.forward_user(user_idx, type_pref, gallery_pref)

    @torch.no_grad()
    def get_exhibition_embeddings(self, features: Dict[str, torch.Tensor]) -> torch.Tensor:
        """批量获取展览向量（推理用）"""
        return self.forward_exhibition(features)