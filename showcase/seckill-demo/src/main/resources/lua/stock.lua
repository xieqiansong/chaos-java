-- ============================================
-- 库存原子扣减 Lua 脚本
-- 返回扣减后的库存余量，库存不足时返回 -1
-- KEYS[1]: 库存 key (seckill:stock:{productId})
-- ARGV[1]: 扣减数量
-- ============================================

local stock_key = KEYS[1]
local quantity = tonumber(ARGV[1])

-- 获取当前库存
local stock = redis.call('get', stock_key)

-- 库存不存在则返回 -2（表示尚未初始化）
if not stock then
    return -2
end

stock = tonumber(stock)

-- 库存不足
if stock < quantity then
    return -1
end

-- 原子扣减
return redis.call('decrby', stock_key, quantity)
