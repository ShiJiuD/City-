package com.cityart.service;

import com.cityart.vo.RecommendExhibitionVO;

import java.util.List;

/**
 * 首页"艺览智荐"推荐服务
 *
 * @author shijiu
 * @since 2026-08-12
 */
public interface RecommendService {

    /**
     * 获取个性化推荐展览列表（07 文档第五章）
     * <p>
     * 登录用户：组装画像 + 候选池 → 调 Python 算法排序 → 查库组装返回；
     * Python 不可用 / 返回异常 → 降级返回热门展览。
     * 游客（userId=null）：直接返回热门展览，不走算法。
     *
     * @param userId 登录用户 ID，null 表示游客
     * @param limit  返回数量上限，null 或 <1 时取默认值 4
     * @return 推荐展览列表；最坏返回热门展览兜底（当前无在展热门展览时可能为空数组，前端需处理空态）
     */
    List<RecommendExhibitionVO> getRecommendations(Long userId, Integer limit);
}
