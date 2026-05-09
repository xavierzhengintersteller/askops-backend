package com.scb.askopt_backend.controller;

import com.scb.askopt_backend.service.DashboardService;
import com.scb.askopt_backend.vo.ApiResponse;
import com.scb.askopt_backend.vo.DashboardStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsVO> dashboardStats() {
        return ApiResponse.success(dashboardService.getDashboardStats());
    }
}
