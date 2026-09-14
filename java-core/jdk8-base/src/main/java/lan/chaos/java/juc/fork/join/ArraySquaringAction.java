package lan.chaos.java.juc.fork.join;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * RecursiveAction（无返回值的并行任务）
 * 场景: 并行修改大型数组（每个元素取平方）
 */
public class ArraySquaringAction extends RecursiveAction {
    // 阈值：小于此值时直接计算
    private static final int THRESHOLD = 10000;
    private final double[] array;
    private final int start;
    private final int end;

    public ArraySquaringAction(double[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void compute() {
        int length = end - start + 1;
        // 1. 如果任务足够小，直接执行
        if (length <= THRESHOLD) {
            for (int i = start; i <= end; i++) {
                array[i] = Math.pow(array[i], 2); // 取平方
            }
            return;
        }

        // 2. 拆分任务
        int mid = start + (end - start) / 2;

        // 3. 创建子任务
        ArraySquaringAction left = new ArraySquaringAction(array, start, mid);
        ArraySquaringAction right = new ArraySquaringAction(array, mid + 1, end);

        // 4. 并行执行子任务（更高效的方式）
        invokeAll(left, right);
    }

    public static void main(String[] args) {
        // 1. 创建测试数据（1000万元素）
        double[] data = new double[10_000_000];
        // 所有元素初始化为2.5
        Arrays.fill(data, 2.5);

        // 2. 创建线程池
        ForkJoinPool pool = new ForkJoinPool();

        // 3. 执行任务
        long startTime = System.nanoTime();
        pool.invoke(new ArraySquaringAction(data, 0, data.length - 1));

        // 4. 验证结果（检查前5个元素）
        System.out.println("验证结果: " + Arrays.toString(Arrays.copyOf(data, 5)));
        System.out.printf("耗时: %d ms%n",
                (System.nanoTime() - startTime) / 1_000_000);

        // 5. 关闭线程池（非必需）
        pool.shutdown();
    }

}
