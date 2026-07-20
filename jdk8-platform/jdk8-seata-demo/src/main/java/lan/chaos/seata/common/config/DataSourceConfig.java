package lan.chaos.seata.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据源配置。
 *
 * <p>为什么用 Druid：</p>
 * <ul>
 *   <li>监控能力（SQL 统计、慢查询），Demo 调试时可以看到每条 SQL 执行情况</li>
 *   <li>Seata AT 模式通过代理 DataSource 拦截 SQL 生成 undo_log，Druid 与 Seata 兼容性好</li>
 * </ul>
 *
 * <p>生产化考量：多数据源时应为每个 DataSource 配置独立的 SeataProxy 和连接池参数。</p>
 *
 * @author chaos
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new DruidDataSource();
    }
}
