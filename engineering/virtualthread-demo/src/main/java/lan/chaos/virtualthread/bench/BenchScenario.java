package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;

import java.util.List;

/**
 * 压测场景契约：一个场景 = 一组「基线 vs 对照」的对比用例。
 * 机制演示层（throughput / pinning / ...）回答「为什么」，
 * 压测场景回答「收益多大、边界在哪」，两者共用 common 层的度量工具。
 */
public interface BenchScenario {

    Scenario id();

    /** 跑完整组对比，返回用例结果。 */
    List<BenchCase> run();

    /** 结论一句话，写进报告的场景小结。 */
    String conclusion();
}
