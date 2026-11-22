package com.zhikao.promotion.config;


import com.zhikao.promotion.utils.MyLockAspect;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Configuration
public class PromotionConfig {

    @Bean
    public ThreadPoolTaskExecutor generateExchangeCodeExecutor() {
        // 创建线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(2);
        // 2.最大线程数
        executor.setMaxPoolSize(5);
        // 3.队列容量
        executor.setQueueCapacity(200);
        // 4.线程名称前缀
        executor.setThreadNamePrefix("exchange-code-handler-");
        // 5.队列满了由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 6.初始化线程池
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor discountSolutionExecutor() {
        // 创建线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(10);
        // 2.最大线程数
        executor.setMaxPoolSize(12);
        // 3.队列容量
        executor.setQueueCapacity(99999);
        // 4.线程名称前缀
        executor.setThreadNamePrefix("discount-solution-calculator-");
        // 5.队列满了由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 6.初始化线程池
        executor.initialize();
        return executor;
    }

/*    @Bean
    public MyLockAspect myLockAspect(RedissonClient redissonClient) {
        return new MyLockAspect(redissonClient);
    }
 */
}
