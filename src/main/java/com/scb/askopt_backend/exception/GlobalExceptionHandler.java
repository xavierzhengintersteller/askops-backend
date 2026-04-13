package com.scb.askopt_backend.exception;

import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleLoginException(LoginException e) {
        return ApiResponse.error(401, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequest(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 业务异常（你要的无Agent、无权限等全部走这里）
     */
    @ExceptionHandler(ApiException.class)
    public ApiResponse<?> handleApiException(ApiException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        e.printStackTrace();
        return ApiResponse.error(500, e.getMessage());
    }

    public static class LoginException extends RuntimeException {
        public LoginException(String message) {
            super(message);
        }
    }
}