package lan.chaos.flink.cdc.sync.mapping;

import com.alibaba.fastjson.JSONObject;
import lan.chaos.flink.cdc.sync.MySQLTableConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
public class OrderMapping implements IMapping, Serializable {
    private MySQLTableConfig mySQLTableConfig = MySQLTableConfig.builder().name("order").pk(Arrays.asList("order_id")).build();

    private String targetTable = "order_new";
    private List<String> targetPrimaryKey = Arrays.asList("order_id");

    /**
     * 映射关系。Debezium 对 MySQL 默认输出小写列名（与源表 DDL 一致），故 sourceField 用小写。
     * finished 字段由 state 推导：state=2 视为已完成
     */
    private List<FieldMapping> fieldMappings = Arrays.asList(
            new FieldMapping("order_id", "order_id", null),
            new FieldMapping("user_id", "user_id", null),
            new FieldMapping("amount", "amount", null),
            new FieldMapping("state", "state", null),
            new FieldMapping("paid_at", "pay_time", null),
            new FieldMapping("finished", "state", OrderMapping::finished_desc)
    );

    public static Object finished_desc(JSONObject json) {
        Integer state = json.getInteger("state");
        return state != null && state == 2 ? 1 : 0;
    }
}
