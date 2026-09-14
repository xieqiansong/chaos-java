package lan.chaos.mybatisplus.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 租户数据实体，用于多租户插件演示（仅此表被租户隔离）。
 */
@Data
@TableName("tenant_data")
public class TenantData {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String bizData;
}
