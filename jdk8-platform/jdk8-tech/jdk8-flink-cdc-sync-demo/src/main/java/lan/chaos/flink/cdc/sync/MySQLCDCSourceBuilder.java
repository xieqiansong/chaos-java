package lan.chaos.flink.cdc.sync;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MySQLCDCSourceBuilder {

    public static <T> MySqlSource<T> buildSource(
            String jobId,
            List<MySQLTableConfig> tables,
            DebeziumDeserializationSchema<T> deserializer,
            StartupOptions startupOption, String subtaskIndex
    ) {
        return buildSource(jobId, tables, deserializer, startupOption, null, subtaskIndex);
    }

    /**
     * 支持指定GTID同步
     */
    public static <T> MySqlSource<T> buildSource(
            String jobId,
            List<MySQLTableConfig> tables,
            DebeziumDeserializationSchema<T> deserializer,
            StartupOptions startupOption,
            String gtidSet, String subtaskIndex
    ) {
        String server = ConfigurationManager.getProperty(jobId, "source.server.address");
        int port = Integer.parseInt(ConfigurationManager.getProperty(jobId, "source.server.port"));
        String database = ConfigurationManager.getProperty(jobId, "source.server.database");
        String username = ConfigurationManager.getProperty(jobId, "source.server.username");
        String password = ConfigurationManager.getProperty(jobId, "source.server.password");
        String serverId = ConfigurationManager.getProperty(jobId, "source.server.server-id");
        String serverTimeZone = ConfigurationManager.getProperty(jobId, "source.server.timezone");

        String tableList = tables.stream()
                .map(t -> database + "." + t.getName())
                .collect(Collectors.joining(","));

        if (deserializer instanceof CommonTableDeserializationSchema) {
            ((CommonTableDeserializationSchema) deserializer).setTables(tables);
        }

        Properties debeziumProps = new Properties();
        debeziumProps.setProperty("gtid.source.filter.dml.events", "true");

        if (gtidSet != null && !gtidSet.trim().isEmpty()) {
            debeziumProps.setProperty("scan.startup.mode", "specific-offset");
            debeziumProps.setProperty("scan.startup.specific-offset.gtid-set", gtidSet);
            startupOption = StartupOptions.specificOffset(gtidSet);
        }

        MySqlSourceBuilder<T> builder = MySqlSource.<T>builder()
                .hostname(server)
                .port(port)
                .databaseList(database)
                .tableList(tableList)
                .username(username)
                .password(password)
                .fetchSize(4 * 1024)
                .includeSchemaChanges(false)
                .serverTimeZone(serverTimeZone)
                .startupOptions(startupOption)
                .debeziumProperties(debeziumProps)
                .deserializer(deserializer)
                .serverId(serverId + subtaskIndex)
                .splitSize(128 * 1024);

        return builder.build();
    }

}
