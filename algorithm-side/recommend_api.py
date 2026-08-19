import logging
from collections import Counter
from typing import List

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

logger = logging.getLogger("recommend_api")
logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

# 打分权重配置
WEIGHT_TYPE_MATCH = 3.0     # 展览类型命中用户收藏类型，按画像中出现次数加权（次数=偏好强度）
WEIGHT_GALLERY_MATCH = 2.0  # 美术馆类型命中用户收藏美术馆类型，同上按次数加权
WEIGHT_FAVORITE = 5.0       # 候选展览在用户收藏列表中：文档标注为"强偏好"，给予最高加权
WEIGHT_HOT = 1.5            # 热门展览加权（兜底保证热门展有基础曝光）
WEIGHT_STATUS = 1.0         # 进行中(status=1) 优先于 未开始(status=0)

# 收藏策略开关：
#   False（默认）= 按文档"强偏好"语义，收藏过的展览加权置顶；
#   True         = 过滤掉已收藏展览（推荐未看过的内容），按产品需求二选一。
FILTER_FAVORITED = False


# 请求数据模型
class UserProfile(BaseModel):
    """用户偏好画像：由 Java 从数据库组装后传入"""
    history_exhibition_types: List[int] = Field(default_factory=list)
    history_gallery_types: List[int] = Field(default_factory=list)
    fav_exhibition_ids: List[int] = Field(default_factory=list)
    browse_exhibition_ids: List[int] = Field(default_factory=list)


class CandidateExhibition(BaseModel):
    """候选展览：Java 传入的候选池全量特征（不含已结束展览）"""
    exhibitionId: int
    galleryId: int
    exhibitionType: int
    galleryType: int
    isHot: int = 0    # 0-普通 1-热门
    status: int = 1   # 0-未开始 1-进行中


class RecommendRequest(BaseModel):
    userId: int
    user_profile: UserProfile
    candidate_exhibitions: List[CandidateExhibition]


# 统一 Result 响应封装
def ok(data, msg="success"):
    return {"code": 1, "msg": msg, "data": data}


def fail(msg="算法服务异常"):
    return {"code": 0, "msg": msg, "data": None}


# 打分逻辑
def model_relevance_score(user_profile: UserProfile, ex: CandidateExhibition) -> float:
    """个性化模型分扩展点（预留）。

    后续把离线训练好的双塔模型接入时，可在此处根据用户画像与候选展览特征
    计算个性化相关性分（建议归一化到 0~1 后叠加）。
    当前为规则打分阶段，固定返回 0，不影响主链路。
    """
    return 0.0


def score_candidates(user_profile: UserProfile,
                     candidates: List[CandidateExhibition]) -> List[int]:
    """按规则对候选池打分，返回按得分降序的展览 ID 数组。

    保证：只返回 candidates 内的 ID；允许返回空数组；结果确定性
    （同分时按 exhibitionId 升序，避免两次请求顺序抖动）。
    """
    type_counter = Counter(user_profile.history_exhibition_types or [])
    gallery_counter = Counter(user_profile.history_gallery_types or [])
    fav_ids = set(user_profile.fav_exhibition_ids or [])

    scored = []
    for ex in candidates:
        # 收藏过滤模式
        if FILTER_FAVORITED and ex.exhibitionId in fav_ids:
            continue

        score = 0.0
        # 1) 类型偏好：命中用户收藏过的展览/美术馆类型，出现次数越多偏好越强
        score += WEIGHT_TYPE_MATCH * type_counter.get(ex.exhibitionType, 0)
        score += WEIGHT_GALLERY_MATCH * gallery_counter.get(ex.galleryType, 0)
        # 2) 强偏好：候选展览本身被用户收藏过
        if ex.exhibitionId in fav_ids:
            score += WEIGHT_FAVORITE
        # 3) 热门加权 + 4) 进行中优先
        if ex.isHot == 1:
            score += WEIGHT_HOT
        if ex.status == 1:
            score += WEIGHT_STATUS
        # 5) 预留：个性化模型分
        score += model_relevance_score(user_profile, ex)

        scored.append((score, ex.exhibitionId))

    scored.sort(key=lambda item: (-item[0], item[1]))
    return [eid for _, eid in scored]


# FastAPI 应用
app = FastAPI(title="艺览智荐推荐算法服务", version="1.0.0",
              description="ciTY展馆 · Java → Python 内部打分接口（不对外暴露）")


@app.exception_handler(RequestValidationError)
async def _validation_handler(request: Request, exc: RequestValidationError):
    """参数校验失败：HTTP 仍返回 200，用业务 code=0 标识，便于 Java 统一降级处理"""
    logger.warning("请求参数校验失败: %s", exc.errors())
    return JSONResponse(status_code=200, content=fail("参数错误"))


@app.exception_handler(Exception)
async def _unhandled_handler(request: Request, exc: Exception):
    logger.exception("未捕获异常")
    return JSONResponse(status_code=200, content=fail("算法服务异常"))


@app.get("/health")
def health():
    """健康检查（文档未定义，供部署探活用，可保留）"""
    return ok(None, "ok")


@app.post("/api/recommend")
def recommend(req: RecommendRequest):
    """Java → Python 推荐打分接口（文档附录二 · 编号 20）"""
    try:
        ids = score_candidates(req.user_profile, req.candidate_exhibitions)
        return ok(ids)
    except Exception:
        logger.exception("推荐打分异常 userId=%s", req.userId)
        return fail("算法服务异常")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)