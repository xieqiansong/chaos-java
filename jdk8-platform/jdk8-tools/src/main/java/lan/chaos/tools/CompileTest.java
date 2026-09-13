package lan.chaos.tools;

import cn.hutool.core.compiler.CompilerUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReflectUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class CompileTest {


    public void delombokTest() throws Exception {
        // 构建命令：使用 java -jar lombok.jar delombok 处理源码
        String lombokPath = "D:\\repository\\org\\projectlombok\\lombok\\1.18.42\\lombok-1.18.42.jar";
        String sourcePath = "D:\\project\\confusion\\chaos-java\\chaos-dynamic\\src\\main\\java\\lan\\chaos\\dynamic\\CompileTest.java";

        ProcessBuilder pb = new ProcessBuilder("java", "-jar", lombokPath, "delombok", "-f", "pretty", "-p", sourcePath);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();
        String processedSource = output.toString();
        Console.log(processedSource);
    }


    public void testCompile() throws Exception {
        // 构建命令：使用 java -jar lombok.jar delombok 处理源码
        String lombokPath = "D:\\repository\\org\\projectlombok\\lombok\\1.18.42\\lombok-1.18.42.jar";
        String sourcePath = "D:\\project\\confusion\\chaos-java\\chaos-dynamic\\src\\main\\java\\lan\\chaos\\dynamic\\CompileTest.java";
        String sourceCode = FileUtil.readUtf8String(sourcePath);
        final ClassLoader classLoader = CompilerUtil.getCompiler(null)
                // 被编译的源码字符串
                .addSource("lan.chaos.dynamic.CompileTest", sourceCode)
                // 编译依赖的库
                .addLibrary(new File(lombokPath))
                .compile();
        Class<?> testClass = classLoader.loadClass("lan.chaos.dynamic.CompileTest");
        Object o = ReflectUtil.newInstance(testClass);

        ReflectUtil.invoke(o, ReflectUtil.getMethod(testClass, "setName", String.class), "ABC");
        Object result = ReflectUtil.invoke(o, ReflectUtil.getMethod(testClass, "getName"));

        Console.log(result);
    }
}
