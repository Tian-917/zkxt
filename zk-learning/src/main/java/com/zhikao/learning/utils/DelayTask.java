package com.zhikao.learning.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelayTask<D> implements Delayed, Serializable {

    private static final long serialVersionUID = 1L;

    // 任务数据
    private D data;
    // 任务截止时间
    private long deadlineNanos;

    /**
     * 构造函数
     * @param data 任务数据
     * @param delayTime 任务延迟时间
     */
    public DelayTask(D data, Duration delayTime) {
        this.data = data;
        this.deadlineNanos = System.nanoTime() + delayTime.toNanos();
    }

    /**
     * 计算剩余有效时间，单位为指定时间单位
     * @param unit 时间单位
     * @return 剩余有效时间，单位为指定时间单位
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(0,deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        long l = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        if (l > 0){
            return 1;
        } else if(l < 0){
            return -1;
        }
        else {
            return 0;
        }
    }
}
