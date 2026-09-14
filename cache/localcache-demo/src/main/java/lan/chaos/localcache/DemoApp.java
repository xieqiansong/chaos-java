package lan.chaos.localcache;

import lan.chaos.localcache.basic.BasicCacheService;
import lan.chaos.localcache.cacheaside.CacheAsideService;
import lan.chaos.localcache.eviction.EvictionCacheService;
import lan.chaos.localcache.expire.ExpireCacheService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印每个能力的「输入→输出」，
 * 想纯看效果不写测试时，直接跑这个 main 即可。
 */
public class DemoApp {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext ctx =
                     SpringApplication.run(LocalCacheApplication.class, args)) {
            section("1. 基础读写 basic", ctx.getBean(BasicCacheService.class).run());
            section("2. 写入过期 expire", ctx.getBean(ExpireCacheService.class).run());
            section("3. 容量淘汰 eviction", ctx.getBean(EvictionCacheService.class).run());
            section("4. 声明式缓存 @Cacheable", ctx.getBean(CacheAsideService.class).run());
        }
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.print(body);
    }
}
