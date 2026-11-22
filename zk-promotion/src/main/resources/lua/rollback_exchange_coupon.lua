-- 兑换消息发送失败时回滚：清除兑换标记并回退用户领取数量
-- KEYS[1] 兑换码标记key（bitmap，offset为兑换码序列号）
-- KEYS[2] 用户领取数量缓存key（hash，field为用户id）
-- ARGV[1] 兑换码序列号
-- ARGV[2] 用户id
redis.call('SETBIT', KEYS[1], tonumber(ARGV[1]), 0)
local count = tonumber(redis.call('HGET', KEYS[2], ARGV[2]) or '0')
if count > 0 then
    redis.call('HINCRBY', KEYS[2], ARGV[2], -1)
end
return 1
