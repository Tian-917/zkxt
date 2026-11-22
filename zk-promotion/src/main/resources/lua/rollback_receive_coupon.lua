-- 领券消息发送失败时回滚：恢复库存并回退用户领取数量
-- KEYS[1] 优惠券缓存key（hash，totalNum为剩余库存）
-- KEYS[2] 用户领取数量缓存key（hash，field为用户id）
-- ARGV[1] 用户id
redis.call('HINCRBY', KEYS[1], 'totalNum', 1)
local count = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
if count > 0 then
    redis.call('HINCRBY', KEYS[2], ARGV[1], -1)
end
return 1
