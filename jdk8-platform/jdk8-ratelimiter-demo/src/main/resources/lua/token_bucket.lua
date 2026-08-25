-- 令牌桶（redis-lua 基准）
-- KEYS[1]=key；ARGV[1]=rate(个/s) ARGV[2]=capacity ARGV[3]=now(ms) ARGV[4]=cost
-- 原子完成：时间窗推进 + 取令牌；返回 "1"/"0"（tostring 规避 Redis8 RESP3 整数回值兼容问题）
local key = KEYS[1]
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])

local b = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(b[1]); if tokens == nil then tokens = capacity end
local ts = tonumber(b[2]); if ts == nil then ts = now end

tokens = math.min(capacity, tokens + (now - ts) / 1000 * rate)
ts = now

local ok = 0
if tokens >= cost then tokens = tokens - cost ok = 1 end

redis.call('HMSET', key, 'tokens', tokens, 'ts', ts)
redis.call('PEXPIRE', key, 30000)

return tostring(ok)