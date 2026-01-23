package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.vo.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {
    @PostMapping("/test")
    public ApiResponse<String> testmethod() {
        System.out.println("test method called");
        return ApiResponse.success("www");
    }
}
