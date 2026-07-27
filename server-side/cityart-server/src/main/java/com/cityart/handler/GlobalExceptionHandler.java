package com.cityart.handler;

import com.cityart.constant.MessageConstant;
import com.cityart.exception.BaseException;
import com.cityart.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 2. 捕获DTO @Valid 参数校验异常（@NotNull/@NotBlank失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result validExceptionHandler(MethodArgumentNotValidException ex) {
        // 拿到第一个校验失败的提示信息
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError.getDefaultMessage();
        log.error("参数校验失败：{}", msg);
        return Result.error(msg);
    }

    /**
     * 3. 兜底：捕获所有其他未知系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result allExceptionHandler(Exception ex) {
        log.error("系统未知异常", ex);
        return Result.error(MessageConstant.SYSTEM_ERROR);
    }
}
