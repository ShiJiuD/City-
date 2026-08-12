package com.cityart.controller.user;

import com.cityart.constant.MessageConstant;
import com.cityart.result.Result;
import com.cityart.service.DetailService;
import com.cityart.vo.ExhibitionDetailVO;
import com.cityart.vo.GalleryDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端展馆/展览详情接口
 * <p>
 * 均需 JWT 登录鉴权（WebMvcConfiguration 中 /api/** 已拦截）。
 */
@Tag(name = "C端展馆展览详情")
@RestController
@RequestMapping("/api/detail")
@RequiredArgsConstructor
@Slf4j
public class DetailController {

    private final DetailService detailService;

    @Operation(summary = "获取展馆详情")
    @GetMapping("/gallery/{id}")
    public Result<GalleryDetailVO> galleryDetail(@PathVariable Long id) {
        log.info("展馆详情请求, id: {}", id);
        GalleryDetailVO vo = detailService.getGalleryDetail(id);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    @Operation(summary = "获取展览详情")
    @GetMapping("/exhibition/{id}")
    public Result<ExhibitionDetailVO> exhibitionDetail(@PathVariable Long id) {
        log.info("展览详情请求, id: {}", id);
        ExhibitionDetailVO vo = detailService.getExhibitionDetail(id);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }
}
