package com.cityart.controller.user;

import com.cityart.constant.AuthMessageConstant;
import com.cityart.constant.MessageConstant;
import com.cityart.context.UserContext;
import com.cityart.dto.FavoriteDTO;
import com.cityart.result.Result;
import com.cityart.service.FavoriteService;
import com.cityart.vo.FavoriteCheckVO;
import com.cityart.vo.FavoriteCountVO;
import com.cityart.vo.FavoritePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端收藏控制器
 * <p>
 * 展览 / 美术馆统一收藏，所有接口需要 JWT 认证（/api/app/ 路径不在拦截器排除名单中），
 * 用户 ID 从 {@link UserContext#getUserId()} 获取。
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Tag(name = "C端收藏模块")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 添加收藏（幂等）
     * <p>
     * 重复收藏返回"已收藏"提示（code=1，不报错）。
     *
     * @param dto 收藏请求体（targetType: 1-展览 2-美术馆, targetId）
     */
    @Operation(summary = "添加收藏")
    @PostMapping("/favorite")
    public Result<?> add(@RequestBody @Validated FavoriteDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("添加收藏请求, userId: {}, targetType: {}, targetId: {}",
                userId, dto.getTargetType(), dto.getTargetId());
        boolean added = favoriteService.addFavorite(userId, dto);
        return Result.success(null, added
                ? AuthMessageConstant.FAVORITE_ADD_SUCCESS
                : AuthMessageConstant.FAVORITE_ALREADY);
    }

    /**
     * 取消收藏（幂等）
     * <p>
     * 未收藏时返回"未收藏"提示（code=1，不报错）。
     *
     * @param dto 收藏请求体
     */
    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorite")
    public Result<?> cancel(@RequestBody @Validated FavoriteDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("取消收藏请求, userId: {}, targetType: {}, targetId: {}",
                userId, dto.getTargetType(), dto.getTargetId());
        boolean canceled = favoriteService.cancelFavorite(userId, dto);
        return Result.success(null, canceled
                ? AuthMessageConstant.FAVORITE_CANCEL_SUCCESS
                : AuthMessageConstant.FAVORITE_NOT_EXIST);
    }

    /**
     * 收藏列表（分页 + 关键词搜索）
     *
     * @param targetType 收藏类型（1-展览 2-美术馆）
     * @param keyword    搜索关键词（模糊匹配标题/名称，可空）
     * @param page       页码，默认 1
     * @param pageSize   每页条数，默认 10，最大 50
     */
    @Operation(summary = "收藏列表（分页）")
    @GetMapping("/favorite/list")
    public Result<FavoritePageVO> list(
            @RequestParam Integer targetType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getUserId();
        log.info("收藏列表请求, userId: {}, targetType: {}, keyword: {}, page: {}, pageSize: {}",
                userId, targetType, keyword, page, pageSize);
        FavoritePageVO vo = favoriteService.getFavoriteList(userId, targetType, keyword, page, pageSize);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 检查收藏状态
     *
     * @param targetType 收藏类型（1-展览 2-美术馆）
     * @param targetId   收藏目标 ID
     */
    @Operation(summary = "检查收藏状态")
    @GetMapping("/favorite/check")
    public Result<FavoriteCheckVO> check(
            @RequestParam Integer targetType,
            @RequestParam Long targetId) {
        Long userId = UserContext.getUserId();
        log.info("收藏状态检查请求, userId: {}, targetType: {}, targetId: {}", userId, targetType, targetId);
        FavoriteCheckVO vo = favoriteService.checkFavorite(userId, targetType, targetId);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 收藏数量统计（个人中心展示）
     */
    @Operation(summary = "收藏数量统计")
    @GetMapping("/favorite/count")
    public Result<FavoriteCountVO> count() {
        Long userId = UserContext.getUserId();
        log.info("收藏数量统计请求, userId: {}", userId);
        FavoriteCountVO vo = favoriteService.countFavorite(userId);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }
}
