package lan.chaos.jdk21features;

import lan.chaos.jdk21features.patternswitch.PatternSwitchDemo;
import lan.chaos.jdk21features.recordpattern.RecordPatternDemo;
import lan.chaos.jdk21features.sequencedcollection.SequencedCollectionDemo;
import lan.chaos.jdk21features.sequencedmap.SequencedMapDemo;
import lan.chaos.jdk21features.virtualthread.VirtualThreadDemo;

/**
 * 控制台入口：分节打印每个 JDK21 特性的「输入 → 输出」。
 */
public class NewFeaturesApp {

    public static void main(String[] args) throws Exception {
        run("虚拟线程 Virtual Threads", VirtualThreadDemo::run);
        run("SequencedCollection 序列集合", SequencedCollectionDemo::run);
        run("SequencedMap 序列映射", SequencedMapDemo::run);
        run("模式匹配 switch", PatternSwitchDemo::run);
        run("Record 模式（解构）", RecordPatternDemo::run);
    }

    private static void run(String name, Runnable r) {
        System.out.println("\n========== " + name + " ==========");
        r.run();
    }
}
