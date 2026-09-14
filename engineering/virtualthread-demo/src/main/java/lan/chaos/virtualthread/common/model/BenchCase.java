package lan.chaos.virtualthread.common.model;

/**
 * 一个对比用例：同一组参数下「基线 vs 对照」两轮压测的结果。
 * 多数场景基线是平台线程池、对照是虚拟线程；但通用性上只要求两者是可比的两种实现
 * （例如 pinning 场景基线与对照都是虚拟线程，差别只在锁的类型）。
 */
public record BenchCase(String name, String params, String baselineLabel, String comparisonLabel,
                        BenchResult baseline, BenchResult comparison) {

    /** 吞吐提升倍数：对照 / 基线。基线吞吐为 0 时返回无穷大。 */
    public double throughputGain() {
        double base = baseline.throughputPerSec();
        return base == 0 ? Double.POSITIVE_INFINITY : comparison.throughputPerSec() / base;
    }

    /** 尾延迟改善倍数：基线 p99 / 对照 p99。对照 p99 为 0 时返回无穷大。 */
    public double p99Gain() {
        double cmp = comparison.p99Millis();
        return cmp == 0 ? Double.POSITIVE_INFINITY : baseline.p99Millis() / cmp;
    }

    @Override
    public String toString() {
        return String.format("%s（%s）%n%s%n%s%n  吞吐提升=%.2f 倍，p99 改善=%.2f 倍",
                name, params, baseline.pretty(), comparison.pretty(), throughputGain(), p99Gain());
    }
}
