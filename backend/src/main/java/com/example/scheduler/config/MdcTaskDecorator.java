package com.example.scheduler.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        var context = MDC.getCopyOfContextMap();
        return () -> {
            var previous = MDC.getCopyOfContextMap();
            if (context == null) MDC.clear();
            else MDC.setContextMap(context);
            try {
                runnable.run();
            } finally {
                if (previous == null) MDC.clear();
                else MDC.setContextMap(previous);
            }
        };
    }
}
