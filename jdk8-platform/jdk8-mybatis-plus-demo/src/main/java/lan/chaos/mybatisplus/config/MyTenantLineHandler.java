package lan.chaos.mybatisplus.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lan.chaos.mybatisplus.common.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/**
 * 多租户处理器（高阶用法）。
 * 仅对 {@code tenant_data} 表注入 {@code tenant_id = ?} 条件；其余表无 tenant_id 列必须忽略，
 * 否则 MP 会给所有 SQL 拼 tenant_id 导致报错。真实系统可按「白名单表 / 忽略表」精细控制。
 */
public class MyTenantLineHandler implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.get();
        return new LongValue(tenantId == null ? 0L : tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !"tenant_data".equalsIgnoreCase(tableName);
    }
}
