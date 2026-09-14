package lan.chaos.seata.common.config;

import io.seata.rm.datasource.xa.DataSourceProxyXA;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * XA 模式数据源代理配置。
 *
 * <p>仅当 {@code seata.xa.enabled=true} 时激活（默认关闭，避免影响 AT/TCC 场景）。
 * 启用步骤：</p>
 * <ol>
 *   <li>application.yml 增加 {@code seata.xa.enabled: true}</li>
 *   <li>关闭 spring-cloud-alibaba 的自动 AT 数据源代理：
 *       {@code seata.enable-auto-data-source-proxy: false}（否则 XA 代理会被二次包装）</li>
 *   <li>数据库需支持 XA 协议</li>
 * </ol>
 *
 * <p>业务代码无需任何改动（见 xa/XaPurchaseService）：
 * @GlobalTransactional 在 XA 数据源下自动走数据库 XA 二阶段提交。</p>
 *
 * @author chaos
 */
@Configuration
@ConditionalOnProperty(name = "seata.xa.enabled", havingValue = "true")
public class XaDataSourceConfig {

    @Bean
    @Primary
    public DataSource xaDataSource(@Qualifier("dataSource") DataSource rawDataSource) {
        return new DataSourceProxyXA(rawDataSource);
    }
}
