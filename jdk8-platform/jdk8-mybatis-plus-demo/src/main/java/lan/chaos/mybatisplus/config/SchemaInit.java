package lan.chaos.mybatisplus.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * 在 Spring 上下文启动时执行 schema.sql + data.sql，初始化 H2 内存库。
 *
 * 为什么不用 spring.sql.init：本仓库该 Demo 的构建环境下 spring.sql.init 未被激活
 * （DataSourceInitializer 未创建，DEBUG 日志亦无执行痕迹），故改为显式、确定可靠的初始化：
 * 在 SchemaInit 这个 bean 的 afterPropertiesSet 阶段、依赖 DataSource 注入后执行脚本。
 * 该阶段早于任何 Mapper 调用，因此测试与 main() 都能拿到已建好的表与种子数据。
 *
 * schema.sql 以 DROP TABLE IF EXISTS 开头，保证重复启动/重复上下文可安全重跑。
 */
@Configuration
public class SchemaInit implements InitializingBean {

    private final DataSource dataSource;
    private final boolean enabled;

    public SchemaInit(DataSource dataSource,
                      @Value("${demo.schema-init-enabled:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/schema.sql"));
        populator.addScript(new ClassPathResource("db/data.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        // 表/列已存在时报错即中断，便于第一时间发现 SQL 问题
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
