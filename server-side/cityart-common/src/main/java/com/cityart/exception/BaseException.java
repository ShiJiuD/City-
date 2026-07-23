package com.cityart.exception;

/**
 * 业务异常基类 — 不直接使用，用具体子类抛
 */
public abstract class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BaseException(String msg) {
        super(msg);
    }

    /** 保留根因，方便排查问题链 */
    public BaseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
