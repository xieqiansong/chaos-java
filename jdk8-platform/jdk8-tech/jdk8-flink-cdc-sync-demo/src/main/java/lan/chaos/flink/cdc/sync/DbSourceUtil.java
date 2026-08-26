package lan.chaos.flink.cdc.sync;

import org.apache.flink.connector.jdbc.JdbcConnectionOptions;

public class DbSourceUtil {


    public static JdbcConnectionOptions jdbcSinkConnectionOptions(String jobId) {
        return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(ConfigurationManager.getProperty(jobId, "sink.jdbc.url"))
                .withDriverName(ConfigurationManager.getProperty(jobId, "sink.jdbc.driver-name"))
                .withUsername(ConfigurationManager.getProperty(jobId, "sink.jdbc.username"))
                .withPassword(ConfigurationManager.getProperty(jobId, "sink.jdbc.password"))
                .build();
    }
}
