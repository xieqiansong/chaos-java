-- ============================================
-- 分桶库存原子扣减 Lua 脚本
-- 遍历所有分桶，依次尝试扣减，降低单 Key 热点竞争
--
-- KEYS[1]: 售罄标记 key (seckill:sold_out:{productId})
-- KEYS[2..N]: 各分桶库存 key (seckill:stock_bucket:{productId}:{0..N-1})
-- ARGV[1]: 分桶数量
-- ARGV[2]: 扣减数量
--
-- 返回值:
--   >0 : 扣减成功的桶索引 (1-based)
--   -1 : 所有分桶均无余量（已售罄）
--   -2 : 无可用分桶配置
-- ============================================

local sold_out_key = KEYS[1]
local bucket_count = tonumber(ARGV[1])
local quantity = tonumber(ARGV[2])

-- 检查售罄标记
local sold_out = redis.call('get', sold_out_key)
if sold_out == '1' then
    return -1
end

if bucket_count <= 0 then
    return -2
end

-- 遍历所有分桶，尝试扣减
for i = 1, bucket_count do
    -- 分桶 key 从 KEYS[2] 开始
    local bucket_key = KEYS[i + 1]
    local stock = redis.call('get', bucket_key)

    if stock then
        stock = tonumber(stock)
        if stock >= quantity then
            local remaining = redis.call('decrby', bucket_key, quantity)
            -- 如果扣减后该桶有余量，直接返回桶索引
            -- 如果扣减后该桶正好为0，也返回桶索引（最后一件）
            if remaining >= 0 then
                return i
            else
                -- 极端情况：并发导致扣减后负值，需要回滚
                redis.call('incrby', bucket_key, quantity)
            end
        end
    end
end

-- 所有分桶均无余量，设置售罄标记
redis.call('set', sold_out_key, '1')
redis.call('expire', sold_out_key, 3600)

return -1
