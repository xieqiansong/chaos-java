package lan.chaos.flink.cdc.sync;

import cn.hutool.core.date.DateUtil;
import lombok.var;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;

import java.time.ZoneId;
import java.util.Date;

/**
 * 对CDC读取过来的的值继续处理
 */
public class ValueHelper {
    // 时区偏移毫秒数：从配置 source.server.timezone（时区名，如 UTC / Asia/Shanghai）计算，
    // 兼容原版数字写法（如 8 表示东八区）。配置为 UTC 时偏移为 0。
    private static final int TIME_OFFSET = computeTimeOffset(ConfigurationManager.getProperty("source.server.timezone"));
    private static final Date START_DATE = DateUtil.parseDate("1970-01-01");

    private static int computeTimeOffset(String tz) {
        if (tz == null || tz.trim().isEmpty()) {
            return 0;
        }
        try {
            // 数字写法（如 "8" / "-5"）：直接当小时偏移
            return (int) (Double.parseDouble(tz.trim()) * 60 * 60 * 1000);
        } catch (NumberFormatException e) {
            // 时区名写法（如 "UTC" / "Asia/Shanghai"）：用 ZoneId 计算当前偏移
            ZoneId zoneId = ZoneId.of(tz.trim());
            return zoneId.getRules().getOffset(java.time.Instant.now()).getTotalSeconds() * 1000;
        }
    }

    public static Object getValFromStruct(Struct struct, Field field) {
        Object reObj = null;
        String objType = field.schema().name();
        if ("io.debezium.time.Date".equals(objType)) {//!!!以后数据库设计尽量不要用Date类型
            var dOffset = struct.getInt32(field.name());
            if (dOffset != null) {
                reObj = DateUtil.offsetDay(START_DATE, dOffset).toJdkDate();
            }
        } else if ("io.debezium.time.Timestamp".equals(field.schema().name())) {
            var dOffset = struct.getInt64(field.name());
            if (dOffset != null) {
                dOffset = dOffset - TIME_OFFSET;
                reObj = new Date(dOffset);
            }
        } else {
            reObj = struct.get(field);
        }
        return reObj;
    }
}
