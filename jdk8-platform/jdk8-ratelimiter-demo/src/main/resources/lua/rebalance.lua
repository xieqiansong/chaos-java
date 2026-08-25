-- 全局窗口计数 / 校准（local-redis 优化）
-- KEYS[1]=rl2:cnt:{tenant}；ARGV[1]=windowMs ARGV[2]=窗口配额(windowSec×globalQps)
-- ARGV[3]=节点数N ARGV[4]=本节点窗口内已放行数 ARGV[5]=now(ms)
-- 返回：本节点下一窗口可分得配额（已扣减全局消耗，可为 0）；tostring 规避 RESP3 整数回值问题
local key = KEYS[1]
local windowMs = tonumber(ARGV[1])
local windowQuota = tonumber(ARGV[2])
local n = tonumber(ARGV[3])
local consumed = tonumber(ARGV[4])
local now = tonumber(ARGV[5])

local c = redis.call('HMGET', key, 'count', 'start')
local count = tonumber(c[1]); if count == nil then count = 0 end
local start = tonumber(c[2]); if start == nil then start = now end

if (now - start) >= windowMs then count = 0 start = now end
count = count + consumed
redis.call('HMSET', key, 'count', count, 'start', start)
redis.call('PEXPIRE', key, 60000)

local remaining = windowQuota - count
if remaining < 0 then remaining = 0 end

return tostring(remaining / n)