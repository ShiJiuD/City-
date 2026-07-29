package com.cityart.controller.user;

import com.cityart.constant.MessageConstant;
import com.cityart.result.Result;
import com.cityart.service.GalleryService;
import com.cityart.service.HomeService;
import com.cityart.vo.GalleryVO;
import com.cityart.vo.HomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "C端首页展览")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final HomeService homeService;
    private final GalleryService galleryService;

    @Operation(summary = "获取首页聚合数据")
    @GetMapping("/home")
    public Result<HomeVO> home(@RequestParam(required = false) String city) {
        log.info("首页聚合数据请求, city: {}", city);
        HomeVO vo = homeService.getHomeData(city);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    @Operation(summary = "获取美术馆列表")
    @GetMapping("/galleries")
    public Result<List<GalleryVO>> galleries(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword) {
        log.info("美术馆列表请求, city: {}, keyword: {}", city, keyword);
        List<GalleryVO> list = galleryService.getGalleryList(city, keyword);
        return Result.success(list, MessageConstant.QUERY_SUCCESS);
    }
}
