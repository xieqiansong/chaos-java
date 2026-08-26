package lan.chaos.flink.cdc.sync;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import io.debezium.data.Envelope;
import lan.chaos.flink.cdc.sync.mapping.IMapping;
import lan.chaos.flink.cdc.sync.mapping.OrderMapping;
import lan.chaos.flink.cdc.sync.mapping.UserMapping;
import lombok.SneakyThrows;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MysqlSyncJob {

    /**
     * MySQL-CDC同步
     */
    public final static String MYSQL_CDC_SYNC = "mysql-cdc-sync";

    /*
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
 */
    public static void main(String[] args) {
        mysqlCdcSync(sys_mappings, "MySQL-CDC同步-All", "0");
    }

    public static List<IMapping> sys_mappings = Arrays.asList(
            new UserMapping(),
            new OrderMapping()
    );

    private static void printTargetTables(List<IMapping> mappings) {
        for (IMapping mapping : mappings) {
            Console.log(mapping.getTargetTable());
        }
    }


    @SneakyThrows
    private static void mysqlCdcSync(List<IMapping> mappings, String jobName, String subtaskIndex) {
        // 创建配置
        Configuration config = new Configuration();

        // 或者设置具体的网络缓冲区大小
        config.setString("taskmanager.memory.network.min", "64mb");
        config.setString("taskmanager.memory.network.max", "64mb");

        // 增加任务管理器总内存
        config.setString("taskmanager.memory.process.size", "1024m");  // 增加总内存

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);
        env.setParallelism(1);

        // 在 Flink 环境中启用 Checkpoint
        CheckpointConfig.defaultCheckpoint(env, MYSQL_CDC_SYNC, 5);

        if (mappings.isEmpty()) {
            System.out.println("No mappings found");
            return;
        }
        MySqlSource<JSONObject> jsonObjectMySqlSource = MySQLCDCSourceBuilder.buildSource(
                MYSQL_CDC_SYNC,
                mappings.stream().map(IMapping::getMySQLTableConfig).collect(Collectors.toList()),
                new CommonTableDeserializationSchema(),
                StartupOptions.initial(), subtaskIndex
        );

        JdbcConnectionOptions connectionOptions = DbSourceUtil.jdbcSinkConnectionOptions(MYSQL_CDC_SYNC);

        DataStream<JSONObject> mySQLCdcSource = env.fromSource(jsonObjectMySqlSource, WatermarkStrategy.noWatermarks(), "MySQL CDC Source").setParallelism(1);
//        mySQLCdcSource.print();

        for (IMapping mapping : mappings) {
            // CREATE READ
            SinkFunction<JSONObject> insertOrUpdateSink = mapping.insertOrUpdateSink(connectionOptions);
            if (Objects.nonNull(insertOrUpdateSink)) {
                mySQLCdcSource.filter(json ->
                                Objects.equals(mapping.getSourceTable(), json.getString(CommonTableDeserializationSchema.TABLE))
                                        && mapping.insertTest().apply(json)
                                        && (Objects.equals(Envelope.Operation.CREATE, json.get(CommonTableDeserializationSchema.OPER))
                                        || Objects.equals(Envelope.Operation.READ, json.get(CommonTableDeserializationSchema.OPER))
                                        || Objects.equals(Envelope.Operation.UPDATE, json.get(CommonTableDeserializationSchema.OPER))))
                        .addSink(insertOrUpdateSink).name("insertOrUpdate_" + mapping.getTargetTable());
            }
            // Delete
            SinkFunction<JSONObject> deleteSink = mapping.deleteSink(connectionOptions);
            if (Objects.nonNull(deleteSink)) {
                mySQLCdcSource.filter(json ->
                                Objects.equals(mapping.getSourceTable(), json.getString(CommonTableDeserializationSchema.TABLE))
                                        && Objects.equals(Envelope.Operation.DELETE, json.get(CommonTableDeserializationSchema.OPER)))
                        .addSink(deleteSink).name("delete_" + mapping.getTargetTable());
            }
        }
        Console.log("开始执行{}", jobName);
        env.execute(jobName);
    }
}
