package lan.chaos.mybatisplus.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import lan.chaos.mybatisplus.common.context.DynamicTableContext;

/**
 * 动态表名处理器（高阶用法）：分表路由。
 * 逻辑表名 {@code log_record} 根据 ThreadLocal 后缀映射到 {@code log_record_2024} / {@code log_record_2025}，
 * 业务无感知。注意要放在 ThreadLocal 用完即 clear，避免线程复用串表。
 */
public class MyDynamicTableNameHandler implements TableNameHandler {

    @Override
    public String dynamicTableName(String sql, String tableName) {
        if ("log_record".equalsIgnoreCase(tableName)) {
            String suffix = DynamicTableContext.getSuffix();
            return "log_record_" + (suffix == null ? "2024" : suffix);
        }
        return tableName;
    }
}
