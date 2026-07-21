-- ============================================
-- 分桶库存原子扣减 Lua 脚本
-- 遍历所有分桶，依次尝试扣减，降低单 Key 热点竞争
-- 同时原子同步总库存（对账用），并在全部售罄时标记
--
-- KEYS[1]: 售罄标记 key (seckill:sold_out:{productId})
-- KEYS[2]: 总库存 key   (seckill:stock:{productId})，扣减时同步递减
-- KEYS[3..N]: 各分桶库存 key (seckill:stock_bucket:{productId}:{0..N-1})
-- ARGV[1]: 分桶数量
-- ARGV[2]: 扣减数量
--
-- 返回值:
--   >0 : 扣减成功的桶索引 (1-based)
--   -1 : 所有分桶均无余量（已售罄）
--   -2 : 无可用分桶配置
-- ============================================

local sold_out_key = KEYS[1]
local stock_key = KEYS[2]
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

-- 遍历所有分桶，尝试扣减（分桶 key 从 KEYS[3] 开始）
for i = 1, bucket_count do
    local bucket_key = KEYS[2 + i]
    local stock = redis.call('get', bucket_key)

    if stock then
        stock = tonumber(stock)
        if stock >= quantity then
            local remaining = redis.call('decrby', bucket_key, quantity)
            -- 扣减成功：同步总库存（对账用）
            redis.call('decrby', stock_key, quantity)
            -- 若扣减后该桶余量 >=0，直接返回桶索引
            if remaining >= 0 then
                -- 检查是否所有分桶均已售罄
                local all_empty = true
                for j = 1, bucket_count do
                    local bk = KEYS[2 + j]
                    local bv = redis.call('get', bk)
                    if bv and tonumber(bv) > 0 then
                        all_empty = false
                        break
                    end
                end
                if all_empty then
                    redis.call('set', sold_out_key, '1')
                    redis.call('expire', sold_out_key, 3600)
                end
                return i
            else
                -- 极端情况：并发导致扣减后负值，需要回滚
                redis.call('incrby', bucket_key, quantity)
                redis.call('incrby', stock_key, quantity)
            end
        end
    end
end

-- 所有分桶均无余量，设置售罄标记
redis.call('set', sold_out_key, '1')
redis.call('expire', sold_out_key, 3600)

return -1
