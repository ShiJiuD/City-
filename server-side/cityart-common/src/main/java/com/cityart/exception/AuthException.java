package com.cityart.exception;

/**
 * 登录、注册相关业务异常
 */
public class AuthException extends BaseException {

    private static final long serialVersionUID = 1L;

    public AuthException(String msg) {
        super(msg);
    }

    public AuthException(String msg, Throwable cause) {
        super(msg, cause);
    }
}