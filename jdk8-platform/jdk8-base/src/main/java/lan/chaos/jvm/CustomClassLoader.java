package lan.chaos.jvm;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;

public class CustomClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 读取 class 文件为 byte[]
        byte[] bytes = loadClassBytes(name);
        Console.log("bytes.len: {}", bytes.length);
        // 断点：观察 defineClass 如何调用 JVM
        Class<?> aClass = defineClass(name, bytes, 0, bytes.length);
        Console.log("aClass: {}", aClass);
        return aClass;
    }

    private byte[] loadClassBytes(String name) {
        return FileUtil.readBytes("/home/ubuntu/project/jdk8u/java_test/TestMain.class");
    }


    public static void main(String[] args) throws Exception {
        CustomClassLoader customClassLoader = new CustomClassLoader();

        Class<?> testMain = customClassLoader.findClass("TestMain");
        Console.log("TestMain: {}", testMain);
    }

}
