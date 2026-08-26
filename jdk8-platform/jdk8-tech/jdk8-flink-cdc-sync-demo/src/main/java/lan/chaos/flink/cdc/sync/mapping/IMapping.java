package lan.chaos.flink.cdc.sync.mapping;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lan.chaos.flink.cdc.sync.CommonTableDeserializationSchema;
import lan.chaos.flink.cdc.sync.MySQLTableConfig;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface IMapping extends Serializable {

    MySQLTableConfig getMySQLTableConfig();

    String getTargetTable();

    List<String> getTargetPrimaryKey();

    List<FieldMapping> getFieldMappings();

    /**
     * 判断是否需要插入
     */
    default SerializableFunction<JSONObject, Boolean> insertTest() {
        return json -> Boolean.TRUE;
    }

    /**
     * 插入或更新 利用 ON DUPLICATE KEY UPDATE 来处理主键冲突
     */
    default SinkFunction<JSONObject> insertOrUpdateSink(JdbcConnectionOptions connectionOptions) {
        List<FieldMapping> fieldMappings = getFieldMappings();
        String insertSql = StrUtil.format("INSERT INTO {} ({}) VALUES ({}) ON DUPLICATE KEY UPDATE {}",
                getTargetTable(),
                fieldMappings.stream().map(FieldMapping::getTargetField).collect(Collectors.joining(", ")),
                fieldMappings.stream().map(f -> "?").collect(Collectors.joining(", ")),
                onDuplicateSql(fieldMappings)
        );

        return JdbcSink.sink(
                insertSql,
                new JdbcStatementBuilder<JSONObject>() {
                    @Override
                    public void accept(PreparedStatement statement, JSONObject record) throws SQLException {
                        AtomicInteger index = new AtomicInteger(1);
                        for (FieldMapping fieldMapping : fieldMappings) {
                            if (fieldMapping.getMapper() == null) {
                                Object object = record.get(fieldMapping.getSourceField());
                                if (Objects.nonNull(object) && Objects.equals("java.nio.HeapByteBuffer", object.getClass().getName())) {
                                    Object hb = ReflectUtil.getFieldValue(object, "hb");
                                    statement.setObject(index.getAndIncrement(), hb);
                                } else {
                                    statement.setObject(index.getAndIncrement(), object);
                                }
                            } else {
                                statement.setObject(index.getAndIncrement(), fieldMapping.getMapper().apply(record));
                            }
                        }
                    }
                },
                JdbcExecutionOptions.builder()
                        .withBatchIntervalMs(100)
                        .withBatchSize(2048)
                        .build(),
                connectionOptions
        );
    }

    default String onDuplicateSql(List<FieldMapping> fieldMappings) {
        return fieldMappings.stream().map(f -> StrUtil.format("{} = VALUES({})", f.getTargetField(), f.getTargetField())).collect(Collectors.joining(", "));
    }

    default SinkFunction<JSONObject> deleteSink(JdbcConnectionOptions connectionOptions) {
        List<String> targetPrimaryKeyList = getTargetPrimaryKey();
        String deleteSql = StrUtil.format("DELETE FROM {} WHERE {}", getTargetTable(), targetPrimaryKeyList.stream().map(f -> StrUtil.format("{} = ?", f))
                .collect(Collectors.joining(" AND ")));

        return JdbcSink.sink(
                deleteSql,
                new JdbcStatementBuilder<JSONObject>() {
                    @Override
                    public void accept(PreparedStatement statement, JSONObject record) throws SQLException {
                        AtomicInteger index = new AtomicInteger(1);
                        List<FieldMapping> fieldMappings = getFieldMappings();
                        for (String pk : targetPrimaryKeyList) {
                            FieldMapping fieldMapping = fieldMappings.stream().filter(f ->
                                    f.getTargetField().equals(pk) ||
                                            Objects.equals(f.getTargetField().replace("`", ""), pk.replace("`", ""))
                            ).findFirst().orElse(null);
                            if (fieldMapping == null) {
                                throw new IllegalArgumentException(StrUtil.format("No field mapping found for primary key {}", pk));
                            }
                            SerializableFunction<JSONObject, Object> mapper = fieldMapping.getMapper();
                            if (mapper != null) {
                                statement.setObject(index.getAndIncrement(), mapper.apply(record));
                            } else {
                                statement.setObject(index.getAndIncrement(), record.getJSONObject(CommonTableDeserializationSchema.OID).get(fieldMapping.getSourceField()));
                            }
                        }
                    }
                },
                connectionOptions
        );
    }

    default String getSourceTable() {
        return getMySQLTableConfig().getName();
    }
}
