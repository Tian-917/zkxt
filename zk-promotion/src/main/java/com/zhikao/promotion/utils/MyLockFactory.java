package com.zhikao.promotion.utils;

import com.zhikao.promotion.enums.MyLockType;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import static com.zhikao.promotion.enums.MyLockType.*;

@Component
public class MyLockFactory {

    private final Map<MyLockType, Function<String,RLock>> lockHandlers;

    public MyLockFactory(RedissonClient redissonClient) {
        this.lockHandlers = new EnumMap<>(MyLockType.class);
        this.lockHandlers.put(RE_ENTRANT_LOCK,redissonClient::getLock);
        this.lockHandlers.put(FAIR_LOCK, redissonClient::getFairLock);
        this.lockHandlers.put(READ_LOCK, name -> redissonClient.getReadWriteLock(name).readLock());
        this.lockHandlers.put(WRITE_LOCK, name -> redissonClient.getReadWriteLock(name).writeLock());
    }

    public RLock getLock(MyLock myLock) {
//        Function<String, RLock> lockHandler = lockHandlers.get(myLock.lockType());
//        RLock lock = lockHandler.apply(myLock.name());
        return lockHandlers.get(myLock.lockType()).apply(myLock.name());
    }
}
