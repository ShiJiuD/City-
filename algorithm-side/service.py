import os
from typing import Optional

import torch

from config import Config
from data_processor import DataProcessor
from model import ExhibitionRecommendationModel
from recommend import ExhibitionRecommender


def _load_checkpoint(model_path: str, map_location):
    try:
        return torch.load(model_path, map_location=map_location, weights_only=False)
    except TypeError:
        return torch.load(model_path, map_location=map_location)


def load_recommender(model_path: str, data_dir: Optional[str] = None,
                     device: str = 'cpu') -> ExhibitionRecommender:
    """

    Args:
        model_path: 训练产出的 final_model.pt 路径
        data_dir:   数据表目录；缺省时使用 checkpoint 里记录的 config.data_dir
        device:     'cpu' 或 'cuda'
    """
    if not os.path.exists(model_path):
        raise FileNotFoundError(f'模型文件不存在: {model_path}')

    checkpoint = _load_checkpoint(model_path, map_location=device)

    config: Config = checkpoint['config']
    if data_dir is not None:
        config.data_dir = data_dir

    # 还原数据处理器：先加载数据表，再恢复训练时的编码器与 id 映射（保证训推一致）
    processor = DataProcessor(config)
    processor.load_data()
    processor.import_state(checkpoint['processor_state'])

    # 按 checkpoint 记录的结构参数重建模型并加载权重
    model = ExhibitionRecommendationModel(config=config, **checkpoint['model_meta'])
    model.load_state_dict(checkpoint['model_state_dict'])
    model.eval()

    recommender = ExhibitionRecommender(config, model, processor, device=device)
    print(f'[service] 模型加载完成: {model_path} '
          f"(epoch={checkpoint.get('epoch')}, loss={checkpoint.get('loss', float('nan')):.4f})")
    return recommender


def recommend_for_user(recommender: ExhibitionRecommender, user_id: int,
                       top_k: Optional[int] = None):
    """便捷调用：返回推荐结果列表；top_k 缺省取 config.top_k"""
    results = recommender.recommend(user_id)
    if top_k is not None:
        results = results[:top_k]
    return results




if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description='展览推荐模型加载与推理演示')
    parser.add_argument('--model', default='./models/final_model.pt', help='模型文件路径')
    parser.add_argument('--data-dir', default=None, help='数据表目录（默认用训练时的配置）')
    parser.add_argument('--user-id', type=int, default=1, help='要推荐的用户 id')
    args = parser.parse_args()

    rec = load_recommender(args.model, args.data_dir)
    items = rec.recommend(args.user_id)
    print(f'用户 {args.user_id} 的推荐结果：')
    for i, item in enumerate(items, 1):
        print(f'  {i}. {item}')