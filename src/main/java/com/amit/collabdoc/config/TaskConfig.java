package com.amit.collabdoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * This configuration class defines a dedicated thread pool for
 * all @Async tasks in the application.
 *
 * By creating a bean named "taskExecutor", we resolve the ambiguity
 * that Spring was warning us about. Spring will automatically use this
 * executor for our DocumentPersistenceService.
 */
@Configuration
@EnableAsync
public class TaskConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // Start with 2 threads
        executor.setMaxPoolSize(5);  // Max 5 threads
        executor.setQueueCapacity(100); // 100 tasks can wait
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
