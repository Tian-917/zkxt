package com.zhikao.promotion.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * 促销模块使用的 Redis Lua 脚本
 */
public final class PromotionRedisScripts {

    /**
     * 领券：校验库存、限领数量并扣减库存
     */
    public static final DefaultRedisScript<Long> RECEIVE_COUPON =
            load("lua/receive_coupon.lua");

    /**
     * 领券消息发送失败时的补偿：恢复库存、回退领取数量
     */
    public static final DefaultRedisScript<Long> ROLLBACK_RECEIVE_COUPON =
            load("lua/rollback_receive_coupon.lua");

    /**
     * 兑换码兑换：校验兑换码是否已使用、限领数量并写入兑换标记
     */
    public static final DefaultRedisScript<Long> EXCHANGE_COUPON =
            load("lua/exchange_coupon.lua");

    /**
     * 兑换消息发送失败时的补偿：清除兑换标记、回退领取数量
     */
    public static final DefaultRedisScript<Long> ROLLBACK_EXCHANGE_COUPON =
            load("lua/rollback_exchange_coupon.lua");

    private PromotionRedisScripts() {
    }

    private static DefaultRedisScript<Long> load(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(location)));
        script.setResultType(Long.class);
        return script;
    }
}
