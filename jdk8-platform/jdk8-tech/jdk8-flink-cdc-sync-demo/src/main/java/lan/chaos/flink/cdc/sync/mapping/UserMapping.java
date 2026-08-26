package lan.chaos.flink.cdc.sync.mapping;

import lan.chaos.flink.cdc.sync.MySQLTableConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
public class UserMapping implements IMapping, Serializable {
    private MySQLTableConfig mySQLTableConfig = MySQLTableConfig.builder().name("user").pk(Arrays.asList("id")).build();

    private String targetTable = "user_new";
    private List<String> targetPrimaryKey = Arrays.asList("id");

    /**
     * 映射关系。Debezium 对 MySQL 默认输出小写列名（与源表 DDL 一致），故 sourceField 用小写。
     */
    private List<FieldMapping> fieldMappings = Arrays.asList(
            new FieldMapping("id", "id", null),
            new FieldMapping("username", "username", null),
            new FieldMapping("display_name", "nickname", null),
            new FieldMapping("status", "status", null),
            new FieldMapping("age", "age", null),
            new FieldMapping("created_at", "create_time", null)
    );
}
