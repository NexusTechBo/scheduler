package com.example.scheduler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    public static final String MAIL_TASK_EXECUTOR = "mailTaskExecutor";
    public static final String WHATSAPP_TASK_EXECUTOR = "whatsappTaskExecutor";

    @Bean(name = MAIL_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor mailTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("mail-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((_, pool) -> log.error(
                "Email task rejected: mail executor saturated (queue size {}), email dropped",
                pool.getQueue().size()));
        return executor;
    }

    @Bean(name = WHATSAPP_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor whatsappTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("whatsapp-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((_, pool) -> log.error(
                "WhatsApp task rejected: executor saturated (queue size {}), message dropped",
                pool.getQueue().size()));
        return executor;
    }
}
