package com.zhikao.promotion.enums;

import com.zhikao.common.exceptions.BizIllegalException;
import com.zhikao.promotion.utils.MyLock;
import org.redisson.api.RLock;

/**
 * 锁的获取策略
 */
public enum MyLockStrategy {
    /**
     * 不重试，获取失败快速结束（返回 false）
     */
    SKIP_FAST() {
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            return lock.tryLock(0, prop.leaseTime(), prop.unit());
        }
    },
    /**
     * 不重试，获取失败快速失败（抛出异常）
     */
    FAIL_FAST() {
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(0, prop.leaseTime(), prop.unit());
            if (!isLock) {
                throw new BizIllegalException("请求太频繁");
            }
            return true;
        }
    },
    /**
     * 无限重试，直到获取锁成功
     */
    KEEP_TRYING() {
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            lock.lock(prop.leaseTime(), prop.unit());
            return true;
        }
    },
    /**
     * 有限重试，重试超时后结束（返回 false）
     */
    SKIP_AFTER_RETRY_TIMEOUT() {
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            return lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
        }
    },
    /**
     * 有限重试，重试超时后抛异常
     */
    FAIL_AFTER_RETRY_TIMEOUT() {
        @Override
        public boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
            if (!isLock) {
                throw new BizIllegalException("请求太频繁");
            }
            return true;
        }
    },
    ;

    public abstract boolean tryLock(RLock lock, MyLock prop) throws InterruptedException;
}
