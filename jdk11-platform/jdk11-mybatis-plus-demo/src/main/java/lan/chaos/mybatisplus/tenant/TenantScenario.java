package lan.chaos.mybatisplus.tenant;

import lan.chaos.mybatisplus.common.context.TenantContext;
import lan.chaos.mybatisplus.common.model.TenantData;
import lan.chaos.mybatisplus.common.mapper.TenantDataMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 场景四：多租户插件（TenantLineInnerInterceptor）。
 * 切换 TenantContext 后，对 tenant_data 的增删改查会自动拼接 tenant_id 条件，
 * 业务代码无需关心，实现数据隔离。
 */
@Service
public class TenantScenario {

    @Resource
    private TenantDataMapper tenantDataMapper;

    public Map<String, Object> tenantIsolation() {
        Map<String, Object> result = new HashMap<>();

        // 租户 1 视角
        TenantContext.set(1L);
        List<TenantData> t1 = tenantDataMapper.selectList(null);

        // 租户 2 视角
        TenantContext.set(2L);
        List<TenantData> t2 = tenantDataMapper.selectList(null);

        TenantContext.clear();

        result.put("tenant1AllSameTenant", t1.stream().allMatch(d -> Long.valueOf(1L).equals(d.getTenantId())));
        result.put("tenant2AllSameTenant", t2.stream().allMatch(d -> Long.valueOf(2L).equals(d.getTenantId())));
        result.put("tenant1Count", t1.size());
        result.put("tenant2Count", t2.size());
        return result;
    }
}
