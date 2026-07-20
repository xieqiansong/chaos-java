package lan.chaos.jvm;

public class TestMain {
    static {
        System.out.println("Static block executed");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Hello from main!");
        // 休眠15秒
        Thread.sleep(15 * 1000);
        Object object = new Object();
    }
}