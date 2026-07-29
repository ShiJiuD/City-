package com.cityart.service.impl;

import com.cityart.service.TokenBlacklistService;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import com.cityart.constant.RedisConstant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {


    private final StringRedisTemplate stringRedisTemplate;

    /** 将 token 加入黑名单，expiration 后自动过期 */
    @Override
    public void addTokenToBlacklist(String token, long expiration) {
        try {
            String key = RedisConstant.KEY_JWT_BLACK_LIST + token;
            stringRedisTemplate.opsForValue().set(key, "1", expiration, TimeUnit.MILLISECONDS);
            log.info("Token 已加入黑名单，TTL={}ms", expiration);
        } catch (RedisException e) {
            // Redis相关所有异常：连接失败、命令报错、超时、集群异常等
            log.error("Redis异常，Token黑名单写入失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            // 非Redis异常，业务代码自身问题
            log.error("未知异常，Token黑名单写入失败: {}", e.getMessage(), e);
        }
    }

    /** 检查 token 是否在黑名单中 */
    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = RedisConstant.KEY_JWT_BLACK_LIST + token;
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (RedisException e) {
            log.error("Token 黑名单查询失败（Redis 不可用）：{}", e.getMessage());
            return false; // Redis 挂了就放行，不影响正常业务
        } catch (Exception e) {
            // 非Redis异常，业务代码自身问题
            log.error("未知异常，Token黑名单查询失败: {}", e.getMessage(), e);
            return false;
        }
    }

}
