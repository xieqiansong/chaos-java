package lan.chaos.mybatisplus.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.mybatisplus.common.model.TenantData;

/**
 * 多租户数据 Mapper，被 TenantLineInnerInterceptor 自动拼接 tenant_id 条件。
 */
public interface TenantDataMapper extends BaseMapper<TenantData> {
}
