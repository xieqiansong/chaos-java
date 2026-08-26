package lan.chaos.idempotent.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 去重表记录（泛化实体，无业务溯源信息）。
 * 统一承载三类去重键：requestId / messageId / bizNo，用 {@code scope} 区分来源场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    /** 去重键：requestId、messageId 或 bizNo，取决于 scope */
    private String key;
    /** 场景域：REQUEST / CONSUME / STATE，避免不同维度键互相碰撞 */
    private String scope;
    /** 首次处理时记录的业务单号（便于对账/排障，非必填） */
    private String bizNo;
    /** 首检时间戳 */
    private LocalDateTime createdAt;
}
