
from dataclasses import dataclass
from typing import Tuple


@dataclass
class Config:
    embedding_dim: int = 64                     # 各塔输出向量维度
    mlp_hidden_dims: Tuple[int, ...] = (256, 128)  # MLP 中间隐层
    dropout_rate: float = 0.3
    learning_rate: float = 0.001
    batch_size: int = 256
    epochs: int = 50
    num_neg_samples: int = 4                    # 每个正样本对应的负样本数（BPR）
    text_embedding_dim: int = 128               # 文本哈希特征维度

    #行为权重
    fav_weight: float = 5.0      # 收藏行为权重更高
    browse_weight: float = 1.0   # 浏览行为权重
    gallery_weight: float = 3.0  # 美术馆类型偏好权重

    #推荐参数
    top_k: int = 4               # 首页推荐 4 个
    diversity_factor: float = 0.3  # MMR 多样性系数
    city_boost: float = 1.25     #最终得分乘数
    relevance_weight: float = 0.7  # 兴趣相关性权重
    urgency_weight: float = 0.3    # 闭幕紧迫度权重

    #数据路径
    data_dir: str = './data'
    model_dir: str = './models'

    #冷启动
    hot_days: int = 30          # 热门新鲜度窗口（天）
    min_interactions: int = 50  # 浏览+收藏达到该阈值才计入热门候选（不足则回退全量）

    #训练
    val_ratio: float = 0.1      # 验证集比例
    save_every: int = 5         # 每 N 个 epoch 存一次 checkpoint
    random_seed: int = 42