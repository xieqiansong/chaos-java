package lan.chaos.localcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 本地缓存标杆 Demo 启动类（A 类模板）。
 * 纯内存、零外部依赖：基于 Caffeine，跑测试 / main 都不需要任何中间件。
 */
@SpringBootApplication
public class LocalCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalCacheApplication.class, args);
    }
}
