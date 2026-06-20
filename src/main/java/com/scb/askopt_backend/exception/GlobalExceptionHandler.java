package com.scb.askopt_backend.exception;

import com.scb.askopt_backend.vo.ApiResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 登录异常 401
    @ExceptionHandler(LoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleLoginException(LoginException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    // 业务异常 200
    @ExceptionHandler(ApiException.class)
    public ApiResponse<?> handleApiException(ApiException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    // 参数异常 400
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException e) {
        return ApiResponse.error(ResultCodeEnum.BAD_REQUEST.getCode(), e.getMessage());
    }

    // 系统异常 500
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        e.printStackTrace();
        return ApiResponse.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }

    // 只保留 LoginException
    @Getter
    public static class LoginException extends RuntimeException {
        private final int code;

        public LoginException(ResultCodeEnum codeEnum) {
            super(codeEnum.getMessage());
            this.code = codeEnum.getCode();
        }
    }
    @Getter
    public static class ApiException extends RuntimeException {
        private final int code;

        // 1. 纯枚举默认文案（原有）
        public ApiException(ResultCodeEnum codeEnum) {
            super(codeEnum.getMessage());
            this.code = codeEnum.getCode();
        }

        // 2. 自定义消息 + 错误码（扩展）
        public ApiException(ResultCodeEnum codeEnum, String message) {
            super(message);
            this.code = codeEnum.getCode();
        }

        // 3. 携带原始异常堆栈（核心：用于日志溯源）
        public ApiException(ResultCodeEnum codeEnum, Throwable cause) {
            super(codeEnum.getMessage(), cause);
            this.code = codeEnum.getCode();
        }

    }
}