package lan.chaos.multilevelcache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 多级缓存配置项。
 * 对应开发计划 M1：L1=Caffeine，L2=Redis Hash(+版本号)，可一键退化为内存版 L2。
 */
@Data
@ConfigurationProperties(prefix = "multilevel-cache")
public class MultilevelCacheProperties {

    /** 是否启用真实 Redis 作为 L2。false 时使用内存版 L2(仅单进程演示)。 */
    private boolean redisEnabled = true;

    @NestedConfigurationProperty
    private Caffeine caffeine = new Caffeine();

    @NestedConfigurationProperty
    private Demo demo = new Demo();

    @Data
    public static class Caffeine {
        private long maximumSize = 10000L;
        private long expireAfterWriteSeconds = 600L;
    }

    @Data
    public static class Demo {
        /** Demo 业务使用的 bizKey 前缀，例如车辆缓存 VEHICLE。 */
        private String vehicleBizKey = "VEHICLE";
    }
}
