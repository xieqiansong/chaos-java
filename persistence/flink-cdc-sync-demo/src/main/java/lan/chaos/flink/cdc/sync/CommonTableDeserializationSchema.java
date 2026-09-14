package lan.chaos.flink.cdc.sync;

import com.alibaba.fastjson.JSONObject;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import io.debezium.data.Envelope;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
@Slf4j
public class CommonTableDeserializationSchema implements DebeziumDeserializationSchema<JSONObject> {
    Map<String, MySQLTableConfig> tables;


    public static String OID = "_oid";
    public static String OPER = "_oper";
    public static String TABLE = "_table";

    public CommonTableDeserializationSchema() {
    }

    public CommonTableDeserializationSchema(List<MySQLTableConfig> tables) {
        this();
        setTables(tables);
    }

    public void setTables(List<MySQLTableConfig> tables) {
        this.tables = new LinkedHashMap<>();
        for (var table : tables) {
            this.tables.put(table.getName(), table);
        }
    }

    public void afterDeserialize(String table, Envelope.Operation operation, Struct before, Struct after, JSONObject result) {
    }


    @Override
    public void deserialize(SourceRecord record, Collector<JSONObject> out) {
        String topic = record.topic();
        String[] split = topic.split("[.]");
        String tableName = split[2];
        var table = this.tables.get(tableName);
        if (Objects.isNull(table) || Objects.isNull(table.getPk())) {
            return;
        }
        Envelope.Operation operation = Envelope.operationFor(record);
        //获取数据本身
        Struct struct = (Struct) record.value();

        Struct after = struct.getStruct("after");
        Struct before = struct.getStruct("before");
        JSONObject jsonObject = new JSONObject();
        if (operation == Envelope.Operation.DELETE) {
            JSONObject _oid = new JSONObject();
            for (String pk : table.getPk()) {
                _oid.put(pk, before.get(pk));
            }
            jsonObject.put(OID, _oid);
            for (String column : table.getDelResolveColumns()) {
                var field = before.schema().field(column);
                jsonObject.put(column, ValueHelper.getValFromStruct(before, field));
            }
        } else {
            var fileds = after.schema().fields();
            for (Field field : fileds) {
                jsonObject.put(field.name(), ValueHelper.getValFromStruct(after, field));
            }
            if (operation == Envelope.Operation.UPDATE) {
                JSONObject _oid = new JSONObject();
                for (String pk : table.getPk()) {
                    _oid.put(pk, before.get(pk));
                }
                jsonObject.put(OID, _oid);
                for (String column : table.getUpdateResolveColumns()) {
                    var field = before.schema().field(column);
                    jsonObject.put("old." + column, ValueHelper.getValFromStruct(before, field));
                }
            }
        }
        jsonObject.put(TABLE, tableName);
        jsonObject.put(OPER, operation);
        afterDeserialize(tableName, operation, before, after, jsonObject);
        try {
            out.collect(jsonObject);
        } catch (Exception e) {
            log.error("Error when collect jsonObject for table {}", tableName, e);
            throw e;
        }
    }

    @Override
    public TypeInformation<JSONObject> getProducedType() {
        return BasicTypeInfo.of(JSONObject.class);
    }
}
