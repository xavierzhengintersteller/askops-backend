package com.scb.tb.askopt_backend.config.Hmac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.tb.askopt_backend.exception.GlobalExceptionHandler.ApiException;
import com.scb.tb.askopt_backend.exception.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Go Agent Podman API 统一客户端
 * 内部自动完成 HMAC 签名，业务CRUD直接调用此类
 */
@Component
@RequiredArgsConstructor
public class GoAgentClient {

    private final RestTemplate restTemplate;
    private final HmacRequestSigner hmacRequestSigner;
    private final ObjectMapper objectMapper;

    /**
     * 通用GET请求（普通Class返回）
     * @param url 完整请求地址
     * @param responseType 响应实体类型
     * @return 响应结果
     */
    public <T> T get(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        String path = getUriPath(url);
        hmacRequestSigner.sign(HttpMethod.GET, path, "", headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        return response.getBody();
    }

    /**
     * 通用GET请求（泛型集合 List/Map 专用）
     */
    public <T> T get(String url, ParameterizedTypeReference<T> typeRef) {
        HttpEntity<Void> requestEntity = buildHmacHeaderEntity(HttpMethod.GET, url, null);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, typeRef);
        return response.getBody();
    }

    /**
     * 通用POST请求（普通Class返回）
     * @param url 完整请求地址
     * @param requestBody 请求体
     * @param responseType 响应实体类型
     * @return 响应结果
     */
    public <T, R> T post(String url, R requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String path = getUriPath(url);
        String bodyStr = toJson(requestBody);
        hmacRequestSigner.sign(HttpMethod.POST, path, bodyStr, headers);

        HttpEntity<R> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        return response.getBody();
    }

    /**
     * 通用POST请求（泛型集合返回专用，预留扩展）
     */
    public <T, R> T post(String url, R requestBody, ParameterizedTypeReference<T> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String path = getUriPath(url);
        String bodyStr = toJson(requestBody);
        hmacRequestSigner.sign(HttpMethod.POST, path, bodyStr, headers);

        HttpEntity<R> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, typeRef);
        return response.getBody();
    }

    /**
     * 通用PUT请求
     */
    public <T, R> T put(String url, R requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String path = getUriPath(url);
        String bodyStr = toJson(requestBody);
        hmacRequestSigner.sign(HttpMethod.PUT, path, bodyStr, headers);

        HttpEntity<R> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
        return response.getBody();
    }

    /**
     * 通用DELETE请求
     */
    public <T> T delete(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        String path = getUriPath(url);
        hmacRequestSigner.sign(HttpMethod.DELETE, path, "", headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, responseType);
        return response.getBody();
    }

    //==================== 私有工具方法 ====================
    /**
     * 构建带HMAC签名的空Body请求Entity（GET/DELETE无请求体接口复用）
     */
    private HttpEntity<Void> buildHmacHeaderEntity(HttpMethod method, String url, String bodyJson) {
        HttpHeaders headers = new HttpHeaders();
        String path = getUriPath(url);
        String body = bodyJson == null ? "" : bodyJson;
        hmacRequestSigner.sign(method, path, body, headers);
        return new HttpEntity<>(headers);
    }

    /**
     * 对象转JSON字符串，序列化失败抛出业务异常并保留原始堆栈
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // 传入原始异常，完整保留报错链路，日志可查看真实原因
            throw new ApiException(ResultCodeEnum.GO_AGENT_SERIALIZE_ERROR, e);
        }
    }

    /**
     * 从完整URL截取接口路径（用于HMAC签名）
     */
    private String getUriPath(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int pathStartIndex = url.indexOf("/", url.indexOf("//") + 2);
        return pathStartIndex > 0 ? url.substring(pathStartIndex) : "/";
    }
}