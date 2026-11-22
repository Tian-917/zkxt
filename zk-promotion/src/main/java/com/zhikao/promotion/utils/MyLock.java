package com.zhikao.promotion.utils;

import com.zhikao.promotion.enums.MyLockStrategy;
import com.zhikao.promotion.enums.MyLockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;



@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyLock {
    // 锁的名称
    String name();
    // 等待时间
    long waitTime() default 1;
    // 锁超时释放时间
    long leaseTime() default-1;
    // 时间单位
    TimeUnit unit() default TimeUnit.SECONDS;
    // 锁的类型
    MyLockType lockType() default MyLockType.RE_ENTRANT_LOCK;
    // 锁的获取策略
    MyLockStrategy lockStrategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;
}
