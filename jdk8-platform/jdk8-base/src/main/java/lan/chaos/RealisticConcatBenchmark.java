package lan.chaos;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Thread)
public class RealisticConcatBenchmark {

    @Param({"10", "100", "1000"})   // 一次测试会分别跑 10、100、1000 长度
    public int length;

    private String[] parts;

    @Setup
    public void setup() {
        parts = new String[5];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = randomString(length);
        }
    }

    /** 直接用 + 拼接 5 个随机字符串 */
    @Benchmark
    public String concatPlus(Blackhole bh) {
        // 注意：必须消费结果，否则可能被优化掉
        String s = parts[0] + parts[1] + parts[2] + parts[3] + parts[4];
        bh.consume(s);
        return s;
    }

    /** 用 StringBuilder 拼接同样的 5 个字符串 */
    @Benchmark
    public String concatBuilder(Blackhole bh) {
        String s = new StringBuilder()
                .append(parts[0]).append(parts[1])
                .append(parts[2]).append(parts[3])
                .append(parts[4]).toString();
        bh.consume(s);
        return s;
    }

    private String randomString(int length) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }

    // 运行入口
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RealisticConcatBenchmark.class.getSimpleName()) // 指定要运行的类
                .forks(1)      // 1次 fork 以获得干净的 JVM 环境
                .warmupIterations(2)  // 预热2次
                .measurementIterations(3) // 正式测量3次
                .build();

        new Runner(opt).run();
    }

}