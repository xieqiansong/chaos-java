package lan.chaos.mybatisplus.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.common.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件集中装配：把本 demo 演示的「分页 / 乐观锁 / 多租户 / 动态表名」四个核心插件串起来。
 *
 * <p>插件执行顺序（务必）：动态表名(最外，先改写表名) → 多租户(拼接 tenant_id) → 乐观锁(带 version 条件)
 * → 分页(最后，包 limit+count)。其中分页必须在最后，否则 count/limit 会作用在被改写前的 SQL 上导致结果错乱。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1) 动态表名：逻辑表 log_record 按年份路由到 log_record_2024 / log_record_2025（分表）
        DynamicTableNameInnerInterceptor dynamicTable = new DynamicTableNameInnerInterceptor();
        dynamicTable.setTableNameHandler(new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                if ("log_record".equals(tableName)) {
                    String suffix = DynamicTableContext.getSuffix();
                    return suffix == null ? tableName : tableName + "_" + suffix;
                }
                return tableName;
            }
        });
        interceptor.addInnerInterceptor(dynamicTable);

        // 2) 多租户：只对 tenant_data 表自动拼接 tenant_id 条件，其余表（user/order/log...）不干扰
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long id = TenantContext.get() == null ? 0L : TenantContext.get();
                return new LongValue(id);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 仅对 tenant_data 生效；其它表没有 tenant_id 列，忽略否则 SQL 报错
                return !"tenant_data".equalsIgnoreCase(tableName);
            }
        }));

        // 3) 乐观锁：带 @Version 的更新自动加 version 条件并自增（防并发覆盖）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 4) 分页：selectPage / IPage 自动 limit + count，必须放最后
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));

        return interceptor;
    }
}
