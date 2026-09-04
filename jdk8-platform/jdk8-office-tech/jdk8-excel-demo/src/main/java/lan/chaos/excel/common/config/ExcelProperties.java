package lan.chaos.excel.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本 Demo 的外部化配置：产物落盘目录。 */
@Data
@Component
@ConfigurationProperties(prefix = "app.excel")
public class ExcelProperties {

    /** 产物输出目录；默认 target/out（target/ 已被 .gitignore 忽略）。 */
    private String outDir = "target/out";
}
