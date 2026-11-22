-- 领券：校验库存、校验限领数量并扣减库存，保证原子性
-- KEYS[1] 优惠券缓存key（hash，totalNum为剩余库存）
-- KEYS[2] 用户领取数量缓存key（hash，field为用户id）
-- ARGV[1] 用户id
-- ARGV[2] 每人限领数量
-- 返回：1-成功，-1-库存不足，-2-超出限领数量
local stock = tonumber(redis.call('HGET', KEYS[1], 'totalNum'))
if stock == nil or stock <= 0 then
    return -1
end

local count = redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
if count > tonumber(ARGV[2]) then
    redis.call('HINCRBY', KEYS[2], ARGV[1], -1)
    return -2
end

redis.call('HINCRBY', KEYS[1], 'totalNum', -1)
return 1
