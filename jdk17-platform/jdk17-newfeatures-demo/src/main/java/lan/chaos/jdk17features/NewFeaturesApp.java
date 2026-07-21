package lan.chaos.jdk17features;

import lan.chaos.jdk17features.hexformat.HexFormatDemo;
import lan.chaos.jdk17features.helpfulnpe.HelpfulNpeDemo;
import lan.chaos.jdk17features.patterninstanceof.PatternInstanceOfDemo;
import lan.chaos.jdk17features.randomgenerator.RandomGeneratorDemo;
import lan.chaos.jdk17features.record.RecordDemo;
import lan.chaos.jdk17features.sealed.SealedDemo;
import lan.chaos.jdk17features.streamtolist.StreamToListDemo;
import lan.chaos.jdk17features.switchexpression.SwitchExpressionDemo;
import lan.chaos.jdk17features.textblock.TextBlockDemo;

/**
 * 控制台入口：分节打印每个 JDK17 特性的「输入 → 输出」。
 */
public class NewFeaturesApp {

    public static void main(String[] args) {
        run("文本块 Text Blocks", TextBlockDemo::run);
        run("Record 记录类", RecordDemo::run);
        run("密封类 Sealed Classes", SealedDemo::run);
        run("instanceof 模式匹配", PatternInstanceOfDemo::run);
        run("Switch 表达式", SwitchExpressionDemo::run);
        run("精确 NullPointerException", HelpfulNpeDemo::run);
        run("Stream.toList / mapMulti", StreamToListDemo::run);
        run("HexFormat 十六进制格式化", HexFormatDemo::run);
        run("RandomGenerator 工厂", RandomGeneratorDemo::run);
    }

    private static void run(String name, Runnable r) {
        System.out.println("\n========== " + name + " ==========");
        r.run();
    }
}
