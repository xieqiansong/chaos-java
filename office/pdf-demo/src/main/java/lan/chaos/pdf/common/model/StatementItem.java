package lan.chaos.pdf.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对账单里的一条明细。
 *
 * <p>金额统一用<b>分</b>（long）表示——这是金额处理的基本功：
 * 用 double 表示金额会在累加与格式化时产生精度误差。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementItem {

    /** 项目名称。 */
    private String project;

    /** 负责人。 */
    private String owner;

    /** 状态（进行中 / 已完成 / 阻塞）。 */
    private String status;

    /** 金额（单位：分）。 */
    private long amountCents;

    /** 样例工厂。 */
    public static StatementItem sample(int i) {
        String status = i % 3 == 0 ? "阻塞" : (i % 3 == 1 ? "进行中" : "已完成");
        return new StatementItem("项目-" + i, "成员-" + (i % 5), status, (long) i * 128_400);
    }

    /** 金额格式化为元（保留两位）。 */
    public String amountYuan() {
        return String.format("%.2f", amountCents / 100.0);
    }
}
