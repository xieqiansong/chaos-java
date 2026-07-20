package lan.chaos.mybatisplus.dynamictable;

import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.entity.LogRecord;
import lan.chaos.mybatisplus.mapper.LogRecordMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 场景五：动态表名插件（DynamicTableNameInnerInterceptor）。
 * 按年份把逻辑表 log_record 路由到 log_record_2024 / log_record_2025，实现分表，业务无感。
 */
@Service
public class DynamicTableScenario {

    @Resource
    private LogRecordMapper logRecordMapper;

    public Map<String, Object> routeByYear() {
        Map<String, Object> result = new HashMap<>();

        DynamicTableContext.setSuffix("2024");
        LogRecord rec2024 = new LogRecord();
        rec2024.setContent("log-new-2024");
        rec2024.setCreateTime(LocalDateTime.now());
        logRecordMapper.insert(rec2024);
        List<LogRecord> r2024 = logRecordMapper.selectList(null);

        DynamicTableContext.setSuffix("2025");
        LogRecord rec2025 = new LogRecord();
        rec2025.setContent("log-new-2025");
        rec2025.setCreateTime(LocalDateTime.now());
        logRecordMapper.insert(rec2025);
        List<LogRecord> r2025 = logRecordMapper.selectList(null);

        DynamicTableContext.clear();

        result.put("count2024", r2024.size());
        result.put("count2025", r2025.size());
        result.put("has2024new", r2024.stream().anyMatch(x -> "log-new-2024".equals(x.getContent())));
        result.put("has2025new", r2025.stream().anyMatch(x -> "log-new-2025".equals(x.getContent())));
        // 路由正确性：2024 表不应包含 2025 的数据
        result.put("noCrossYear", r2024.stream().noneMatch(x -> "log-new-2025".equals(x.getContent())));
        return result;
    }
}
