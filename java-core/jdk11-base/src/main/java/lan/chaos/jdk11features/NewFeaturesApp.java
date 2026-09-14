package lan.chaos.jdk11features;

import lan.chaos.jdk11features.files.FilesDemo;
import lan.chaos.jdk11features.httpclient.HttpClientDemo;
import lan.chaos.jdk11features.optional.OptionalDemo;
import lan.chaos.jdk11features.predicate.PredicateDemo;
import lan.chaos.jdk11features.stream.StreamDemo;
import lan.chaos.jdk11features.string.StringDemo;
import lan.chaos.jdk11features.toarray.ToArrayDemo;
import lan.chaos.jdk11features.varlambda.VarLambdaDemo;

/**
 * 控制台入口：分节打印每个 JDK11 特性的「输入 → 输出」。
 */
public class NewFeaturesApp {

    public static void main(String[] args) {
        run("String 新方法", StringDemo::run);
        run("Files.readString/writeString", FilesDemo::run);
        run("Optional 增强", OptionalDemo::run);
        run("Stream takeWhile/dropWhile/ofNullable", StreamDemo::run);
        run("Predicate.not", PredicateDemo::run);
        run("Collection.toArray(IntFunction)", ToArrayDemo::run);
        run("var in lambda", VarLambdaDemo::run);
        run("HttpClient", HttpClientDemo::run);
    }

    private static void run(String name, Runnable r) {
        System.out.println("\n========== " + name + " ==========");
        r.run();
    }
}
