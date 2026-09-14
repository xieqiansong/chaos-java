package lan.chaos.tools;

public class CPUKiller {

    /**
     * 消耗CPU的方法，n越大执行时间越久
     *
     * @param n 控制计算复杂度的参数
     * @return 计算结果的模拟值
     */
    public static long consumeCPU(int n) {
        long result = 0;

        // 计算复杂度与n的平方成正比，确保n越大时间越久
        for (int i = 0; i < n * n; i++) {
            // 执行一些计算密集型操作
            result += fibonacci(i % 30);  // 限制斐波那契数列大小避免溢出
            result += isPrime(i) ? 1 : 0; // 素数判断增加计算量
        }

        return result;
    }

    /**
     * 计算斐波那契数列（递归实现，消耗CPU）
     */
    private static long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    /**
     * 判断是否为素数（计算密集型）
     */
    private static boolean isPrime(int number) {
        if (number <= 1) return false;
        if (number == 2) return true;
        if (number % 2 == 0) return false;

        for (int i = 3; i * i <= number; i += 2) {
            if (number % i == 0) return false;
        }
        return true;
    }
}