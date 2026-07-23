package com.cityart.constant;

/**
 * Redis 相关常量
 * 包含 Key前缀、操作提示文案、过期时间常量
 */
public class RedisConstant {

    // ====================== Key 前缀（核心！防止不同业务key冲突） ======================
    /** JWT 黑名单前缀 */
    public static final String KEY_JWT_BLACK_LIST = "jwt:blacklist:";
    /** 验证码前缀（短信/图形验证码） */
    public static final String KEY_VERIFY_CODE = "verify:code:";
    /** 登录用户信息缓存前缀（可选） */
    public static final String KEY_LOGIN_USER = "login:user:";
    /** 接口限流前缀 */
    public static final String KEY_RATE_LIMIT = "rate:limit:";
    /** 分布式锁前缀 */
    public static final String KEY_DISTRIBUTE_LOCK = "dist:lock:";

    // ====================== 过期时间 单位：毫秒 ======================
    /** 验证码有效期 5分钟 */
    public static final long CODE_TTL = 5 * 60 * 1000L;
    /** 分布式锁等待时间 10秒 */
    public static final long LOCK_WAIT_TTL = 10 * 1000L;
    /** 分布式锁持有过期时间 30秒 */
    public static final long LOCK_HOLD_TTL = 30 * 1000L;

    // ====================== Redis操作提示文案 ======================
    // JWT黑名单
    public static final String BLACKLIST_ADD_SUCCESS = "Token黑名单加入成功";
    public static final String BLACKLIST_ADD_FAIL = "Token黑名单写入失败";
    public static final String BLACKLIST_QUERY_FAIL = "黑名单查询失败";
    public static final String TOKEN_IN_BLACKLIST = "Token已失效，请重新登录";

    // 验证码
    public static final String CODE_SEND_SUCCESS = "验证码发送成功";
    public static final String CODE_SEND_FAIL = "验证码发送失败";
    public static final String CODE_NOT_EXIST = "验证码不存在或已过期";
    public static final String CODE_ERROR = "验证码不正确";
    public static final String CODE_VERIFY_SUCCESS = "验证码校验通过";

    // 分布式锁
    public static final String LOCK_GET_SUCCESS = "成功获取分布式锁";
    public static final String LOCK_GET_FAIL = "获取分布式锁失败，资源繁忙";
    public static final String LOCK_RELEASE_SUCCESS = "分布式锁释放成功";
    public static final String LOCK_RELEASE_FAIL = "分布式锁释放失败";

    // 缓存通用
    public static final String CACHE_SET_SUCCESS = "缓存写入成功";
    public static final String CACHE_SET_FAIL = "缓存写入失败";
    public static final String CACHE_GET_SUCCESS = "缓存查询成功";
    public static final String CACHE_GET_FAIL = "缓存查询失败";
    public static final String CACHE_DEL_SUCCESS = "缓存删除成功";
    public static final String CACHE_DEL_FAIL = "缓存删除失败";
    public static final String CACHE_DATA_NULL = "缓存无数据";

    // 限流
    public static final String RATE_LIMIT_PASS = "放行，未触发限流";
    public static final String RATE_LIMIT_BLOCK = "请求过于频繁，请稍后再试";
}