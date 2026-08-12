package com.cityart.controller.user;

import com.cityart.constant.MessageConstant;
import com.cityart.result.Result;
import com.cityart.service.ExhibitionService;
import com.cityart.vo.ExhibitionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端展览列表接口（正在展出 / 即将展出 / 往期）
 * <p>
 * 三个接口参数、返回一致，仅时间段不同：
 * 按 start_date / end_date 实时判断，不依赖 status 字段。
 */
@Tag(name = "C端展览列表")
@RestController
@RequestMapping("/api/app/exhibitions")
@RequiredArgsConstructor
@Slf4j
public class ExhibitionController {

    /** 时间段常量（与 ExhibitionMapper.xml 的 choose 分支对应） */
    private static final String PERIOD_CURRENT = "current";
    private static final String PERIOD_FUTURE = "future";
    private static final String PERIOD_PAST = "past";

    private final ExhibitionService exhibitionService;

    @Operation(summary = "正在展出的展览列表")
    @GetMapping("/current")
    public Result<List<ExhibitionListVO>> current(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city) {
        log.info("正在展出列表请求, keyword: {}, type: {}, city: {}", keyword, type, city);
        return Result.success(exhibitionService.getExhibitionList(PERIOD_CURRENT, keyword, type, city),
                MessageConstant.QUERY_SUCCESS);
    }

    @Operation(summary = "即将展出的展览列表")
    @GetMapping("/future")
    public Result<List<ExhibitionListVO>> future(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city) {
        log.info("即将展出列表请求, keyword: {}, type: {}, city: {}", keyword, type, city);
        return Result.success(exhibitionService.getExhibitionList(PERIOD_FUTURE, keyword, type, city),
                MessageConstant.QUERY_SUCCESS);
    }

    @Operation(summary = "往期展出的展览列表")
    @GetMapping("/past")
    public Result<List<ExhibitionListVO>> past(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city) {
        log.info("往期展出列表请求, keyword: {}, type: {}, city: {}", keyword, type, city);
        return Result.success(exhibitionService.getExhibitionList(PERIOD_PAST, keyword, type, city),
                MessageConstant.QUERY_SUCCESS);
    }
}
