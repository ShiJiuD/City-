package com.cityart.utils;

import com.cityart.constant.RedisConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类（Cache Aside 通用能力）
 * <p>
 * 统一封装：JSON 序列化读写、TTL 随机抖动（防雪崩）、空值缓存（防穿透）、
 * 互斥锁（防击穿，判等释放防误删）。
 * <p>
 * 用法示例：
 * <pre>
 * // 读缓存（首页等无 404 语义的场景）
 * HomeVO vo = redisCacheUtil.get(key, HomeVO.class);
 * // 写缓存（TTL + 随机抖动）
 * redisCacheUtil.set(key, vo, RedisConstant.HOME_CACHE_TTL);
 *
 * // 详情等需要区分"空值缓存命中"的场景：先 getRaw 判断空串，再 parse 反序列化
 * String json = redisCacheUtil.getRaw(key);
 * if (json.isEmpty()) { /* DB 不存在，返回 404 *&#47; }
 * GalleryDetailVO vo = redisCacheUtil.parse(json, GalleryDetailVO.class);
 * </pre>
 *
 * @author shijiu
 * @since 2026-08-11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCacheUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 读缓存，返回原始 JSON 字符串
     *
     * @param key 缓存 key
     * @return 原始字符串；未命中返回 null
     */
    public String getRaw(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 读缓存并反序列化为指定类型
     *
     * @param key  缓存 key
     * @param type 目标类型
     * @return 反序列化结果；未命中 / 空值 / 反序列化失败返回 null
     */
    public <T> T get(String key, Class<T> type) {
        String json = getRaw(key);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return parse(json, type);
    }

    /**
     * 反序列化 JSON 字符串（失败降级：log.warn + 返回 null，由调用方走查库）
     *
     * @param json 缓存中的 JSON 字符串（非空）
     * @param type 目标类型
     * @return 反序列化结果；失败返回 null
     */
    public <T> T parse(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("缓存反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 写缓存：JSON 序列化 + TTL + 随机抖动（防缓存雪崩）
     * <p>
     * 序列化失败时不阻断业务：log.warn 后跳过写缓存。
     *
     * @param key       缓存 key
     * @param value     待缓存对象
     * @param ttlMillis 基础 TTL（毫秒），实际 TTL = 基础值 + 随机抖动
     */
    public <T> void set(String key, T value, long ttlMillis) {
        try {
            String json = objectMapper.writeValueAsString(value);
            long ttl = jitterTtl(ttlMillis);
            stringRedisTemplate.opsForValue().set(key, json, ttl, TimeUnit.MILLISECONDS);
            log.info("缓存写入, key: {}, ttl: {}ms", key, ttl);
        } catch (JsonProcessingException e) {
            log.warn("缓存序列化失败, key: {}, 跳过写缓存", key, e);
        }
    }

    /**
     * 写空值缓存（短 TTL，防缓存穿透）：DB 不存在的 id 缓存为空串，后续请求直接 404 不打库
     *
     * @param key 缓存 key
     */
    public void setEmpty(String key) {
        stringRedisTemplate.opsForValue().set(key, "", RedisConstant.EMPTY_CACHE_TTL, TimeUnit.MILLISECONDS);
        log.info("空值缓存写入, key: {}, ttl: {}ms", key, RedisConstant.EMPTY_CACHE_TTL);
    }

    /**
     * 抢互斥锁（setnx，锁 TTL 兜底防持锁方崩溃）
     * <p>
     * 成功者负责重建缓存，其他线程拿不到锁时休眠重试或直接查库。
     *
     * @param key 锁 key
     * @return 成功返回锁标识（释放时必须原样传入 unlock）；失败返回 null
     */
    public String tryLock(String key) {
        String lockValue = UUID.randomUUID().toString();
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, lockValue, RedisConstant.DETAIL_LOCK_TTL, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(flag) ? lockValue : null;
    }

    /**
     * 释放互斥锁：仅当 value 仍是自己的锁标识时才删除（防误删他人锁）
     *
     * @param key       锁 key
     * @param lockValue tryLock 返回的锁标识
     */
    public void unlock(String key, String lockValue) {
        String current = stringRedisTemplate.opsForValue().get(key);
        if (lockValue != null && lockValue.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }

    /**
     * TTL 加随机抖动（0 ~ TTL_JITTER_RANGE），防止大量 key 同时过期造成缓存雪崩
     *
     * @param ttlMillis 基础 TTL（毫秒）
     * @return 实际 TTL（毫秒）
     */
    private long jitterTtl(long ttlMillis) {
        return ttlMillis + ThreadLocalRandom.current().nextLong(0, RedisConstant.TTL_JITTER_RANGE);
    }
}
