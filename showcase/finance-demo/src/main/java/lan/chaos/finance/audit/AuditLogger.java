package lan.chaos.finance.audit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 不可篡改的审计日志（P4，对应对话「审计留痕 / 权限分离」）。
 *
 * <p>WHY：谁、在什么时间、做了什么操作，必须可追溯且<b>不可篡改</b>。
 * 实现要点：
 * <ul>
 *   <li><b>append-only</b>：只暴露 {@code record()}，没有修改/删除接口；</li>
 *   <li><b>只返回快照</b>：{@code snapshot()} 返回不可修改副本，外部拿不到内部引用；</li>
 *   <li><b>写入即脱敏</b>：明细参数在 record 内部强制脱敏，从源头杜绝明文入日志。</li>
 * </ul>
 *
 * <p>生产环境应落独立审计库 + 只追加权限（甚至 WORM 存储 / 哈希链防篡改），
 * 这里用内存 List 演示语义。
 */
public class AuditLogger {

    private final List<AuditEntry> entries = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    /**
     * 记录一条审计日志。{@code rawDetail} 会被强制脱敏后写入。
     */
    public AuditEntry record(String operator, String action, String target, String rawDetail) {
        AuditEntry entry = new AuditEntry();
        entry.setId(sequence.incrementAndGet());
        entry.setOperator(operator);
        entry.setAction(action);
        entry.setTarget(target);
        entry.setMaskedDetail(MaskUtils.maskCard(rawDetail));
        entry.setOccurredAt(LocalDateTime.now());
        synchronized (entries) {
            entries.add(entry);
        }
        return entry;
    }

    /** 返回只读快照（不可修改内部列表）。 */
    public List<AuditEntry> snapshot() {
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    public int size() {
        return entries.size();
    }
}
