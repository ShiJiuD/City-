package com.cityart.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface TokenBlacklistService {

    void addTokenToBlacklist(String token, long expiration);

    boolean isTokenBlacklisted(String token);
}
