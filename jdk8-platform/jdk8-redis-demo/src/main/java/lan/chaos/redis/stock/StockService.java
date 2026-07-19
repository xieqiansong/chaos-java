package lan.chaos.redis.stock;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 库存扣减（Lua）★★☆。
 *
 * <p><b>为什么用 Lua：</b>超卖本质是「读库存→判断→扣减」非原子导致并发下重复扣。
 * 用 Lua 把这段逻辑原子执行，返回 1 成功 / 0 库存不足 / -1 未初始化。</p>
 *
 * <p><b>坑点：</b>这只是单机原子扣减；真正分布式库存还要考虑 Redis 与 DB 最终一致、
 * 预扣/回滚、热点 key（可用分段库存缓解）。</p>
 */
@Service
public class StockService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_DEDUCT_SCRIPT =
            "local stock = tonumber(redis.call('get', KEYS[1]))\n" +
            "if stock == nil then return -1 end\n" +
            "if stock <= 0 then return 0 end\n" +
            "redis.call('decr', KEYS[1])\n" +
            "return 1";

    public void init(String key, int stock) {
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.CACHE_KEY + "stock:" + key, String.valueOf(stock));
    }

    public long deduct(String key) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(STOCK_DEDUCT_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(RedisKeyConstants.CACHE_KEY + "stock:" + key));
        return result == null ? -2 : result;
    }
}
