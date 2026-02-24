package com.scb.askopt_backend.exception;

import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 登录异常
     */
    @ExceptionHandler(LoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleLoginException(LoginException e) {
        return ApiResponse.error(401, e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 兜底异常（系统异常）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        e.printStackTrace(); // 生产建议改为日志
        return ApiResponse.error(500, e.getMessage());
    }


    /**
     * ===============================
     * 内部定义业务异常（集中管理）
     * ===============================
     */
    public static class LoginException extends RuntimeException {
        public LoginException(String message) {
            super(message);
        }
    }
}
