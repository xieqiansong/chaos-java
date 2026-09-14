package lan.chaos.flink.cdc.sync.mapping;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.Serializable;

@Data
public class FieldMapping implements Serializable {
    private String targetField;
    private String sourceField;
    private SerializableFunction<JSONObject, Object> mapper;


    public FieldMapping(String targetField, String sourceField, SerializableFunction<JSONObject, Object> mapper) {
        this.targetField = targetField;
        this.sourceField = sourceField;
        this.mapper = mapper;
    }

    public String getSourceField() {
        if (StrUtil.isBlank(sourceField)) {
            return sourceField;
        }
        return sourceField.replaceAll("`", "");
    }
}
