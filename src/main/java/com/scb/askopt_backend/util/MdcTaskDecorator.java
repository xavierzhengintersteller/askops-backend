package com.scb.askopt_backend.util;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}