package com.cityart.result;

import com.cityart.constant.MessageConstant;
import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = MessageConstant.OP_SUCCESS;
        return r;
    }
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = MessageConstant.OP_SUCCESS;
        r.data = data;
        return r;
    }
    public static <T> Result<T> success(T data, String msg) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = msg;
        r.data = data;
        return r;
    }
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = msg;
        return r;
    }
}