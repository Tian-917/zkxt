package com.zhikao.promotion.utils;

import com.zhikao.common.utils.BooleanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor
public class RedisLock {

    private final String key;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取锁
     * @param leaseTime
     * @param unit
     * @return
     */
    public boolean tryLock(long leaseTime, TimeUnit unit) {
        // 1.获取线程名称
        String value = Thread.currentThread().getName();
        // 2.获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, leaseTime, unit);
        // 3.返回结果
        return BooleanUtils.isTrue(success);
    }

    /**
     * 释放锁
     */
    public void unLock() {
        stringRedisTemplate.delete(key);
    }
}
