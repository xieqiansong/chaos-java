package lan.chaos.java.base;


public class ExceptionDemo {
    public static void simpleTryCatchFinally() {
        try {
            testNPE();
        } catch (Exception e) {
            System.out.println("Exception");
        } finally {
            System.out.println("Finally");
        }
    }

    public static void testNPE() {
        Object o = null;
        System.out.println(o.toString());
    }
}
