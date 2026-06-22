package com.scb.askopt_backend;

import com.scb.askopt_backend.config.Hmac.GoAgentClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class GoAgentClientTests {
    @Autowired
    private GoAgentClient goAgentClient;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void should_pass_trace_id_to_go_agent() {

        MDC.put("traceId", "trace-test-001");

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("ok"));

        goAgentClient.get(
                "http://localhost:8090/api/podman/containers",
                String.class
        );

        ArgumentCaptor<HttpEntity<?>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                anyString(),
                any(),
                captor.capture(),
                eq(String.class)
        );

        HttpHeaders headers = captor.getValue().getHeaders();

        System.out.println(headers);

        assertEquals(
                "trace-test-001",
                headers.getFirst("X-Trace-Id")
        );
    }
}
