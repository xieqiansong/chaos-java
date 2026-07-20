package lan.chaos.localcache.cacheaside;

import lan.chaos.localcache.common.constant.CacheNameConstants;
import lan.chaos.localcache.common.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 能力四：声明式缓存（Cache-Aside + @Cacheable）。
 *
 * <p>WHY：生产里绝大多数缓存只是「把方法返回值缓存起来」，手写 put/get 太啰嗦。
 * 用 @Cacheable 声明后，Spring 在方法调用前查缓存、命中直接返回，未命中才执行方法并把结果写回。
 * 这背后就是 Cache-Aside 模式：读时回填、写时（此处未演示）由调用方失效缓存。
 *
 * <p>关键 API：@Cacheable(cacheNames=..., key=...)。缓存名取自 common/constant，杜绝魔法值。
 * 生产坑：被注解的方法必须是 Spring Bean 的公共方法（AOP 代理），同类内部调用不生效；
 * 缓存的是「返回值」，注意对象引用与序列化（分布式缓存需序列化）。
 */
@Service
public class CacheAsideService {

    private int dbCalls = 0;

    /** 模拟一次「查库」：只有缓存未命中才会真的执行，执行次数记到 dbCalls。 */
    @Cacheable(cacheNames = CacheNameConstants.COMPUTE, key = "#id")
    public User load(Long id) {
        dbCalls++;
        return User.sample(id);
    }

    public int getDbCalls() {
        return dbCalls;
    }

    /** 控制台 / 测试统一入口：同一 id 调用两次，第二次应命中缓存、不再查库。 */
    public String run() {
        User first = load(1L);
        User second = load(1L);
        StringBuilder sb = new StringBuilder();
        sb.append("第 1 次 load(1) -> ").append(first).append('\n');
        sb.append("第 2 次 load(1) -> ").append(second).append("  (同一对象，来自缓存)\n");
        sb.append("实际查库次数 dbCalls=").append(dbCalls).append("  (2 次调用只查了 1 次)\n");
        return sb.toString();
    }
}
