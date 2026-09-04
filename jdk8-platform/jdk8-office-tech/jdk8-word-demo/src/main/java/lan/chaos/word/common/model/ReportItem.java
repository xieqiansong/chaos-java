package lan.chaos.word.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报告中的一条工作项（用于表格与模板行复制演示）。
 *
 * <p>字段全部是虚构数据，不含任何真实业务信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportItem {

    /** 项目名称。 */
    private String project;

    /** 负责人。 */
    private String owner;

    /** 状态（进行中 / 已完成 / 阻塞）。 */
    private String status;

    /** 进度百分比（字符串，避免 DecimalFormat 干扰演示）。 */
    private String progress;

    /** 样例工厂。 */
    public static ReportItem sample(int i) {
        String status = i % 3 == 0 ? "阻塞" : (i % 3 == 1 ? "进行中" : "已完成");
        return new ReportItem("项目-" + i, "成员-" + (i % 5), status, (i * 7 % 100) + "%");
    }
}
