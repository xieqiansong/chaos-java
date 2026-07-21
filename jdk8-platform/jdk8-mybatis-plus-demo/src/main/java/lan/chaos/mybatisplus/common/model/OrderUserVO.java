package lan.chaos.mybatisplus.common.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 联表分页结果 VO（非表实体，仅承载 SELECT 结果）。
 */
@Data
public class OrderUserVO {
    private String userName;
    private BigDecimal amount;
    private LocalDateTime orderTime;
}
