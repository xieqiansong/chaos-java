package lan.chaos.jdk8features;

import lan.chaos.jdk8features.base64.Base64Demo;
import lan.chaos.jdk8features.completablefuture.CompletableFutureDemo;
import lan.chaos.jdk8features.datetime.DateTimeDemo;
import lan.chaos.jdk8features.defaultmethod.DefaultMethodDemo;
import lan.chaos.jdk8features.lambda.LambdaDemo;
import lan.chaos.jdk8features.methodreference.MethodReferenceDemo;
import lan.chaos.jdk8features.optional.OptionalDemo;
import lan.chaos.jdk8features.stream.StreamDemo;
import lan.chaos.jdk8features.stringjoiner.StringJoinerDemo;

/**
 * 控制台入口：分节打印每个 JDK8 特性的「输入 → 输出」，便于把玩与观察。
 * 也可直接跑各 {@code *Demo.run()} 或单元测试。
 */
public class NewFeaturesApp {

    public static void main(String[] args) {
        run("Lambda 表达式", LambdaDemo::run);
        run("Stream API", StreamDemo::run);
        run("Optional", OptionalDemo::run);
        run("java.time 日期时间", DateTimeDemo::run);
        run("接口默认/静态方法", DefaultMethodDemo::run);
        run("方法引用", MethodReferenceDemo::run);
        run("CompletableFuture", CompletableFutureDemo::run);
        run("Base64", Base64Demo::run);
        run("StringJoiner", StringJoinerDemo::run);
    }

    private static void run(String name, Runnable r) {
        System.out.println("\n========== " + name + " ==========");
        r.run();
    }
}
