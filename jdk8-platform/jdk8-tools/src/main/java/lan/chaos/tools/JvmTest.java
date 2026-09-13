package lan.chaos.tools;

import cn.hutool.core.lang.Console;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class JvmTest {

    /**
     * 查看对象内存信息
     * viewObjectMemoryInformation
     */
    public static void showObjMemInfo(Object obj) {
        if (Objects.isNull(obj)) {
            return;
        }
        // 1. 查看类布局和单个实例大小
        Console.log("=== Class Layout ===");
        Console.log(ClassLayout.parseClass(obj.getClass()).toPrintable());

        // 2. 查看具体对象的内存占用（浅大小）
        Console.log("=== Object Size (Shallow) ===");
        Console.log(ClassLayout.parseInstance(obj).toPrintable());

        // 3. 查看对象及其引用的完整内存占用
        Console.log("=== Object Graph Size (Deep) ===");
        Console.log(GraphLayout.parseInstance(obj).toPrintable());
        Console.log("Total size: " + GraphLayout.parseInstance(obj).totalSize() + " bytes");

        // 4. 获取具体数值
        long shallowSize = ClassLayout.parseInstance(obj).instanceSize();
        long deepSize = GraphLayout.parseInstance(obj).totalSize();

        Console.log("Shallow size: " + shallowSize + " bytes");
        Console.log("Deep size: " + deepSize + " bytes");
    }

    public static void main(String[] args) {
    }

    public void test() {
        //返回 JVM 堆大小
        long initalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        //返回 JVM 堆的最大内存
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;

        System.out.println("-Xms : " + initalMemory + "M");
        System.out.println("-Xmx : " + maxMemory + "M");

        System.out.println("系统内存大小：" + initalMemory * 64 / 1024 + "G");
        System.out.println("系统内存大小：" + maxMemory * 4 / 1024 + "G");


        Object lock = new Object();

        // 无锁状态
        System.out.println("===== 无锁状态 =====");
        System.out.println(ClassLayout.parseInstance(lock).toPrintable());

        synchronized (lock) {
            // 同步块内（轻量级锁/重量级锁）
            System.out.println("===== 持有锁状态 =====");
            System.out.println(ClassLayout.parseInstance(lock).toPrintable());
        }
    }

    public void oom_test() {
        List<String> list = new ArrayList<>();

        while (true) {
            list.add("hello world" + UUID.randomUUID());
        }
    }

}
