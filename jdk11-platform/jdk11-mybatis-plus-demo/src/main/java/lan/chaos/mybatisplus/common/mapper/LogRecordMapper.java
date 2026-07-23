package lan.chaos.mybatisplus.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.mybatisplus.common.model.LogRecord;

/**
 * 日志 Mapper，演示动态表名分表（逻辑表 log_record → 物理表 log_record_2024/_2025）。
 */
public interface LogRecordMapper extends BaseMapper<LogRecord> {
}
