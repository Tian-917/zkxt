package com.zhikao.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    /**
     * 学习记录延迟任务的消费线程池
     */
    @Bean("learningRecordExecutor")
    public Executor learningRecordExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(4);
        // 2.最大线程数
        executor.setMaxPoolSize(8);
        // 3.队列容量
        executor.setQueueCapacity(200);
        // 4.线程名称前缀
        executor.setThreadNamePrefix("learning-record-");
        // 5.队列满了由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 6.初始化线程池
        executor.initialize();
        return executor;
    }
}
