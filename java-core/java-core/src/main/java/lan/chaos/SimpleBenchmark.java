package lan.chaos;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)          // 每个测试线程拥有独立的实例
public class SimpleBenchmark {

    private String prefix;
    private String suffix;

    @Setup                     // 每次测试前初始化（类似 @Before）
    public void setup() {
        prefix = "Hello";
        suffix = "World";
    }

    @Benchmark                 // 标记为基准测试方法
    public String stringConcat() {
        // 注意：必须返回一个值，或者通过 Blackhole 消费结果，
        // 否则 JIT 可能把整个调用优化掉（死代码消除）
        return prefix + " " + suffix;
    }

    @Benchmark
    public String stringBuilder() {
        return new StringBuilder()
                .append(prefix)
                .append(" ")
                .append(suffix)
                .toString();
    }

    // 运行入口
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(SimpleBenchmark.class.getSimpleName()) // 指定要运行的类
                .forks(1)      // 1次 fork 以获得干净的 JVM 环境
                .warmupIterations(2)  // 预热2次
                .measurementIterations(3) // 正式测量3次
                .build();

        new Runner(opt).run();
    }
}