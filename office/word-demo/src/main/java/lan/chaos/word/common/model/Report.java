package lan.chaos.word.common.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 样例数据模型：项目周报。全模块共用，便于表格、模板填充横向演示。
 *
 * <p>自带 {@link #sample()} 工厂，调用方无需自己准备输入。字段全部是虚构数据，不含任何真实业务信息。
 */
@Data
public class Report {

    /** 报告标题。 */
    private String title;

    /** 作者。 */
    private String author;

    /** 部门。 */
    private String department;

    /** 日期（字符串，避免 Date 依赖干扰演示）。 */
    private String date;

    /** 结论。 */
    private String conclusion;

    /** 工作项列表（表格 / 模板行复制用）。 */
    private List<ReportItem> items = new ArrayList<>();

    /** 样例工厂：造一份默认周报。 */
    public static Report sample() {
        Report report = new Report();
        report.setTitle("2026 年第二季度项目周报");
        report.setAuthor("张三");
        report.setDepartment("研发一部");
        report.setDate("2026-06-30");
        report.setConclusion("整体进展顺利，2 个项目存在阻塞需协调资源。");
        for (int i = 1; i <= 3; i++) {
            report.getItems().add(ReportItem.sample(i));
        }
        return report;
    }

    /** 样例工厂：造 n 份（内容相同，便于压测/循环演示）。 */
    public static List<Report> samples(int n) {
        List<Report> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(sample());
        }
        return list;
    }
}
