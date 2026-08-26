package lan.chaos.bitmap.model;

/**
 * 统计结果行（指标 / 值 / 说明），供场景输出统一对齐。
 */
public class StatResult {

    private final String metric;
    private final String value;
    private final String note;

    public StatResult(String metric, String value, String note) {
        this.metric = metric;
        this.value = value;
        this.note = note;
    }

    public String getMetric() {
        return metric;
    }

    public String getValue() {
        return value;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return String.format("%-24s %-16s %s", metric, value, note);
    }
}
