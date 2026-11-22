-- 兑换码兑换：校验兑换码是否已使用、校验限领数量，并原子地写入兑换标记
-- KEYS[1] 兑换码标记key（bitmap，offset为兑换码序列号）
-- KEYS[2] 用户领取数量缓存key（hash，field为用户id）
-- ARGV[1] 兑换码序列号
-- ARGV[2] 用户id
-- ARGV[3] 每人限领数量
-- 返回：1-成功，-1-兑换码已兑换，-2-超出限领数量
local serialNum = tonumber(ARGV[1])
if redis.call('GETBIT', KEYS[1], serialNum) == 1 then
    return -1
end

local count = redis.call('HINCRBY', KEYS[2], ARGV[2], 1)
if count > tonumber(ARGV[3]) then
    redis.call('HINCRBY', KEYS[2], ARGV[2], -1)
    return -2
end

redis.call('SETBIT', KEYS[1], serialNum, 1)
return 1
