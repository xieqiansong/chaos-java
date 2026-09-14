package lan.chaos.mqtt.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 演示实体：传感器读数。
 *
 * <p>所有场景共用，自带 {@link #sample(String)} 工厂造默认数据，调用方无需自己准备输入。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading implements Serializable {

    private String sensorId;
    private double value;
    private String unit;
    private long ts;

    public static SensorReading sample(String sensorId) {
        return SensorReading.builder()
                .sensorId(sensorId)
                .value(21.5)
                .unit("C")
                .ts(System.currentTimeMillis())
                .build();
    }

    /** 手工拼 JSON（避免为演示引入 Jackson 依赖；生产建议用 JSON 库） */
    public String toJson() {
        return String.format("{\"sensorId\":\"%s\",\"value\":%s,\"unit\":\"%s\",\"ts\":%d}",
                sensorId, value, unit, ts);
    }
}
