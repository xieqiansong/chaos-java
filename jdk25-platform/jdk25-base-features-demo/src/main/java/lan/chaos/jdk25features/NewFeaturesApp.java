package lan.chaos.jdk25features;

import lan.chaos.jdk25features.flexiblector.FlexibleConstructorDemo;
import lan.chaos.jdk25features.gatherers.GatherersDemo;
import lan.chaos.jdk25features.instancemain.InstanceMainDemo;
import lan.chaos.jdk25features.moduleimport.ModuleImportDemo;
import lan.chaos.jdk25features.primitivepattern.PrimitivePatternDemo;

/**
 * 控制台入口：分节打印每个 JDK25 特性的「输入 → 输出」。
 */
public class NewFeaturesApp {

    public static void main(String[] args) {
        run("模块导入声明 import module", ModuleImportDemo::run);
        run("灵活构造器体 Flexible Constructor Bodies", FlexibleConstructorDemo::run);
        run("隐式声明类 / 实例 main", () -> new InstanceMainDemo().main());
        run("Stream Gatherers", GatherersDemo::run);
        run("原始类型模式 Primitive Patterns", PrimitivePatternDemo::run);
    }

    private static void run(String name, Runnable r) {
        System.out.println("\n========== " + name + " ==========");
        r.run();
    }
}
