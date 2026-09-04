package lan.chaos.pdf.common.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 样例数据模型：项目对账单。全模块共用，便于表格、绘制、提取横向演示。
 *
 * <p>自带 {@link #sample()} 工厂，调用方无需自己准备输入。字段全部是虚构数据，不含任何真实业务信息。
 */
@Data
public class Statement {

    /** 单据标题。 */
    private String title;

    /** 客户名称。 */
    private String customer;

    /** 账期。 */
    private String period;

    /** 备注/结论。 */
    private String remark;

    /** 明细行（表格绘制用）。 */
    private List<StatementItem> items = new ArrayList<>();

    /** 样例工厂：造一份默认对账单。 */
    public static Statement sample() {
        Statement s = new Statement();
        s.setTitle("2026 年第二季度项目对账单");
        s.setCustomer("混沌科技（示例客户）");
        s.setPeriod("2026-04-01 ~ 2026-06-30");
        s.setRemark("本单为示例数据，金额与项目均为虚构，仅用于演示 PDF 生成。");
        for (int i = 1; i <= 6; i++) {
            s.getItems().add(StatementItem.sample(i));
        }
        return s;
    }

    /** 明细金额合计（分）。 */
    public long totalCents() {
        long sum = 0;
        for (StatementItem item : items) {
            sum += item.getAmountCents();
        }
        return sum;
    }
}
