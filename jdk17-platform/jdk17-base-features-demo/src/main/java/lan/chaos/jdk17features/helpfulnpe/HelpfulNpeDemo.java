package lan.chaos.jdk17features.helpfulnpe;

/**
 * 精确的 NullPointerException（JEP 358，JDK15 定稿，JDK17 默认开启）：NPE 信息可直接指出"哪个变量为 null"。
 *
 * <p>WHY：旧 NPE 只有一行 "NullPointerException"，定位问题要肉眼回溯一长串 {@code a.b.c.d} 调用链。
 * 现在 JVM 会告诉你 "Cannot read field "name" because "a.b.c" is null"，直接命中根因。
 * 注意：消息默认开启（{@code -XX:+ShowCodeDetailsInExceptionMessages}），但不同 JVM / 版本文案略有差异，测试只断言"抛出了带信息的 NPE"。
 */
public class HelpfulNpeDemo {

    static class A {
        B b = new B();
    }

    static class B {
        C c; // 故意为 null
    }

    static class C {
        String name = "x";
    }

    public static void run() {
        try {
            A a = new A();
            // a.b 非 null，a.b.c 为 null，再取 .name 触发 NPE
            String ignore = a.b.c.name;
            System.out.println(ignore);
        } catch (NullPointerException e) {
            System.out.println("JDK17 精确定位 NPE: " + e.getMessage());
        }
    }
}
