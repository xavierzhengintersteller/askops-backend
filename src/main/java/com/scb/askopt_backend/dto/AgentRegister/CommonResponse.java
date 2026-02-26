package com.scb.askopt_backend.dto.AgentRegister;

import lombok.Data; /**
 * 通用响应DTO（复用）
 */
@Data
public class CommonResponse {
    private Integer code;       // 状态码：0成功，非0失败
    private String message;     // 提示信息
    private Object data;        // 响应数据
}
