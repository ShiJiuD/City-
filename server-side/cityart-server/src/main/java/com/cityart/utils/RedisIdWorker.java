package com.cityart.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一 ID 生成器（基于 Redis 自增）
 * <p>
 * 参考黑马 Redis 实战篇 §3.2。
 * <p>
 * ID 组成（64 bit）：
 * <pre>
 * | 符号位 |     时间戳 (31 bit)      |    序列号 (32 bit)     |
 * |   0   | 当前秒 - 基准时间戳      | Redis INCR 每日自增    |
 * </pre>
 * 支持每天 2^32 ≈ 43 亿个 ID，可用 69 年。
 *
 * @author shijiu
 * @since 2026-08-04
 */
@Component
public class RedisIdWorker {

    /** 基准时间戳（2026-01-01 00:00:00 UTC），让 ID 更短 */
    private static final long BEGIN_TIMESTAMP = 1767196800L;

    /** 序列号占用的位数 */
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成下一个唯一 ID
     *
     * @param keyPrefix Redis key 前缀（如 "order"）
     * @return 全局唯一 ID
     */
    public long nextId(String keyPrefix) {
        // 1. 时间戳：当前秒 - 基准时间
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 序列号：Redis INCR，按天分区，自动重置
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue()
                .increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接：时间戳 << 32 | 序列号
        return timestamp << COUNT_BITS | count;
    }
}
