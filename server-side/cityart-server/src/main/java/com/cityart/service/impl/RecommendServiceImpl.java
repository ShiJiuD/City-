package com.cityart.service.impl;

import com.cityart.dto.CandidateExhibitionDTO;
import com.cityart.dto.RecommendPythonRequestDTO;
import com.cityart.dto.RecommendPythonResponseDTO;
import com.cityart.dto.UserProfileDTO;
import com.cityart.entity.Exhibition;
import com.cityart.mapper.ExhibitionMapper;
import com.cityart.mapper.FavoriteMapper;
import com.cityart.service.RecommendService;
import com.cityart.vo.RecommendCandidateVO;
import com.cityart.vo.RecommendExhibitionVO;
import com.cityart.vo.UserFavoriteTypeVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 首页"艺览智荐"推荐服务实现（07 文档 7.x）
 * <p>
 * 调用链路：画像组装 → 候选池查询 → POST Python 算法（RestClient，3s 超时）→
 * 拿回排序 ID 数组 → 查库组装 VO（保持 Python 顺序、去重、limit 截断）。
 * 任何一步失败（Python 不可用/异常/候选池空/游客）→ 降级返回热门展览。
 *
 * @author shijiu
 * @since 2026-08-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    /** 默认推荐数量上限 */
    private static final int DEFAULT_LIMIT = 4;

    /** 推荐数量上限（防滥用） */
    private static final int MAX_LIMIT = 50;

    private final ExhibitionMapper exhibitionMapper;
    private final FavoriteMapper favoriteMapper;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    /** Python 算法服务地址（application.yml cityart.recommend.python-url） */
    @Value("${cityart.recommend.python-url}")
    private String pythonUrl;

    /** Python 算法服务超时（毫秒） */
    @Value("${cityart.recommend.timeout}")
    private long pythonTimeout;

    /** 已配置超时的 RestClient，@PostConstruct 初始化 */
    private RestClient restClient;

    @PostConstruct
    public void initRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) pythonTimeout);
        factory.setReadTimeout((int) pythonTimeout);
        this.restClient = restClientBuilder.requestFactory(factory).build();
        log.info("推荐服务 RestClient 初始化完成, python-url: {}, timeout: {}ms", pythonUrl, pythonTimeout);
    }

    @Override
    public List<RecommendExhibitionVO> getRecommendations(Long userId, Integer limit) {
        int safeLimit = resolveLimit(limit);
        log.info("推荐请求, userId: {}, limit: {}", userId, safeLimit);

        // 游客：不组装画像，直接热门兜底（07 文档第八章）
        if (userId == null) {
            log.info("游客访问推荐, 返回热门展览兜底");
            return hotFallback(safeLimit);
        }

        // 候选池查询（营业美术馆下未结束的展览）
        List<RecommendCandidateVO> candidates = exhibitionMapper.selectRecommendCandidates();
        if (candidates.isEmpty()) {
            log.warn("推荐候选池为空, 降级返回热门展览");
            return hotFallback(safeLimit);
        }

        // 组装画像 + 请求体，调 Python 算法
        UserProfileDTO profile = buildProfile(userId);
        List<Long> ids = callPython(buildRequest(userId, profile, candidates));
        if (ids == null || ids.isEmpty()) {
            log.warn("Python 推荐无有效结果, 降级返回热门展览");
            return hotFallback(safeLimit);
        }

        // 去重 + 过滤候选池外脏 ID + limit 截断（Stream.distinct 保留 Python 顺序的首次出现位置）
        Map<Long, RecommendCandidateVO> candidateIndex = candidates.stream()
                .collect(Collectors.toMap(RecommendCandidateVO::getExhibitionId, c -> c));
        List<Long> orderedIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(candidateIndex::containsKey)
                .distinct()
                .limit(safeLimit)
                .toList();
        if (orderedIds.isEmpty()) {
            log.warn("Python 返回的 ID 均不在候选池, 降级返回热门展览");
            return hotFallback(safeLimit);
        }

        // selectBatchIds 不保序，按 Python 顺序用 Map 重排组装 VO
        Map<Long, Exhibition> exhibitionIndex = exhibitionMapper.selectBatchIds(orderedIds).stream()
                .collect(Collectors.toMap(Exhibition::getId, e -> e));
        List<RecommendExhibitionVO> result = orderedIds.stream()
                .map(id -> toVO(exhibitionIndex.get(id), candidateIndex.get(id)))
                .filter(Objects::nonNull)
                .toList();
        if (result.isEmpty()) {
            log.warn("Python 返回的展览均已不存在, 降级返回热门展览");
            return hotFallback(safeLimit);
        }
        return result;
    }

    /**
     * 组装用户偏好画像（07 文档 7.1）：收藏展览 ID + 展览类型 + 美术馆类型，均去重；浏览记录本期恒空
     */
    private UserProfileDTO buildProfile(Long userId) {
        List<UserFavoriteTypeVO> rows = favoriteMapper.selectUserFavoriteTypes(userId);
        return UserProfileDTO.builder()
                .favExhibitionIds(rows.stream()
                        .map(UserFavoriteTypeVO::getFavExhibitionId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .historyExhibitionTypes(rows.stream()
                        .map(UserFavoriteTypeVO::getExhibitionType)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .historyGalleryTypes(rows.stream()
                        .map(UserFavoriteTypeVO::getGalleryType)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                // 本期无浏览记录数据源，恒为空数组（07 文档：后续接入浏览记录表后再填充）
                .browseExhibitionIds(List.of())
                .build();
    }

    /**
     * 组装 Java → Python 请求体（07 文档 6.1）
     */
    private RecommendPythonRequestDTO buildRequest(Long userId, UserProfileDTO profile,
                                                   List<RecommendCandidateVO> candidates) {
        List<CandidateExhibitionDTO> candidateDtos = candidates.stream()
                .map(c -> CandidateExhibitionDTO.builder()
                        .exhibitionId(c.getExhibitionId())
                        .galleryId(c.getGalleryId())
                        .exhibitionType(c.getExhibitionType())
                        .galleryType(c.getGalleryType())
                        .isHot(c.getIsHot())
                        .status(c.getStatus())
                        .build())
                .toList();
        return RecommendPythonRequestDTO.builder()
                .userId(userId)
                .userProfile(profile)
                .candidateExhibitions(candidateDtos)
                .build();
    }

    /**
     * 调用 Python 算法服务（07 文档 6.2）
     * <p>
     * 连接失败/超时/非 2xx/响应非 JSON/code!=1/data 为空 → 返回 null，由调用方降级。
     * RestClientException 覆盖：ResourceAccessException（连接/超时）、
     * RestClientResponseException（非 2xx）、HttpMessageNotReadableException（解析失败）。
     */
    private List<Long> callPython(RecommendPythonRequestDTO request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            log.info("调用 Python 推荐服务, url: {}, 候选池数量: {}", pythonUrl, request.getCandidateExhibitions().size());
            log.debug("Python 推荐请求体: {}", json);
            RecommendPythonResponseDTO resp = restClient.post()
                    .uri(pythonUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(RecommendPythonResponseDTO.class);
            if (resp == null || !Integer.valueOf(1).equals(resp.getCode()) || resp.getData() == null) {
                log.warn("Python 推荐服务返回异常响应: {}", resp == null ? "null" : resp.getMsg());
                return null;
            }
            return resp.getData();
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Python 推荐服务调用失败, 降级返回热门展览: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 热门展览兜底（07 文档第八章）：is_hot=1 且正在展出，按 sort_order DESC，截断到 limit
     */
    private List<RecommendExhibitionVO> hotFallback(int limit) {
        return exhibitionMapper.selectHotRecommendExhibitions().stream()
                .limit(limit)
                .toList();
    }

    /**
     * 组装返回 VO：galleryName 取候选池行，price 为 null 时兜底 0（免费展）
     */
    private RecommendExhibitionVO toVO(Exhibition exhibition, RecommendCandidateVO candidate) {
        if (exhibition == null) {
            return null;
        }
        return RecommendExhibitionVO.builder()
                .id(exhibition.getId())
                .posterImage(exhibition.getPosterImage())
                .title(exhibition.getTitle())
                .subtitle(exhibition.getSubtitle())
                .galleryName(candidate == null ? null : candidate.getGalleryName())
                .type(exhibition.getType())
                .price(exhibition.getPrice() == null ? BigDecimal.ZERO : exhibition.getPrice())
                .build();
    }

    /**
     * limit 兜底：null 或 <1 取默认 4，超 50 截断防滥用
     */
    private int resolveLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
