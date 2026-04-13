package com.scb.askopt_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    @Bean("containerExecutor")
    public Executor containerExecutor() {
        AtomicInteger threadCounter = new AtomicInteger(1);
        return new ThreadPoolExecutor(
                16,                  // 核心
                40,                  // 最大
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 排队缓冲，避免瞬间创建线程
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("container-task-" + threadCounter.getAndIncrement());
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}