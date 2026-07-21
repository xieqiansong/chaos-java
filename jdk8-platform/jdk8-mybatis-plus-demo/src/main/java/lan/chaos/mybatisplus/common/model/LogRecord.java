package lan.chaos.mybatisplus.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志实体，用于动态表名演示。
 * 逻辑表名 log_record，运行时由动态表名插件映射到 log_record_2024 / log_record_2025。
 */
@Data
@TableName("log_record")
public class LogRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private LocalDateTime createTime;
}
