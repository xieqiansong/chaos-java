package lan.chaos.batchwriter.bench;

/**
 * 单次压测的运行参数。
 */
public class BenchOptions {

    /** 并发汇聚线程数 */
    public int threads = 8;

    /** 目标汇聚速率（条/秒）；flood=true 时忽略 */
    public int rate = 5000;

    /** 持续秒数 */
    public int durationSec = 15;

    /** 满速压测（忽略 rate） */
    public boolean flood = false;

    public static BenchOptions of(int threads, int rate, int durationSec, boolean flood) {
        BenchOptions o = new BenchOptions();
        o.threads = threads;
        o.rate = rate;
        o.durationSec = durationSec;
        o.flood = flood;
        return o;
    }

    public String describe() {
        return String.format("threads=%d rate=%d duration=%ds flood=%s", threads, rate, durationSec, flood);
    }
}