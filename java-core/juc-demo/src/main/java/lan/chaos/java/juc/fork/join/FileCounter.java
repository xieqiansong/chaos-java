package lan.chaos.java.juc.fork.join;

import java.io.File;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CountedCompleter（任务链回调）
 * 场景: 实现并行分治的目录遍历（统计所有txt文件数量）
 */
public class FileCounter extends CountedCompleter<Void> {
    private final File dir;
    private final AtomicInteger totalCount;

    // 根任务构造器
    public FileCounter(File dir) {
        this.dir = dir;
        this.totalCount = new AtomicInteger(0);
    }

    // 子任务构造器
    private FileCounter(CountedCompleter<?> parent, File dir, AtomicInteger totalCount) {
        super(parent);
        this.dir = dir;
        this.totalCount = totalCount;
    }


    @Override
    public void compute() {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        // 设置挂起计数器（需要等待的子任务数）
        setPendingCount(files.length - 1);

        // 文件处理逻辑
        for (int i = 1; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                // 创建子任务并提交
                new FileCounter(this, file, totalCount).fork();
            } else if (file.getName().endsWith(".java")) {
                totalCount.incrementAndGet();
            }
        }

        // 处理第一个文件（使用当前线程）
        File firstFile = files[0];
        if (firstFile.isDirectory()) {
            // 嵌套任务（触发CountedCompleter机制）
            new FileCounter(this, firstFile, totalCount).compute();
        } else if (firstFile.getName().endsWith(".txt")) {
            totalCount.incrementAndGet();
        }

        // 减少挂起计数（tryComplete会在计数=0时触发onCompletion）
        tryComplete();
    }

    @Override
    public void onCompletion(CountedCompleter<?> caller) {
        // 所有子任务完成后执行
        System.out.println("Completed: " + dir.getPath());
    }

    public int getTotalCount() {
        return totalCount.get();
    }


    public static void main(String[] args) {
        // 准备测试目录（可替换为实际路径）
        File root = new File("D:/data");

        // 创建任务
        FileCounter task = new FileCounter(root);

        // 执行任务（使用默认线程池）
        ForkJoinPool.commonPool().invoke(task);

        System.out.println("Total .txt files: " + task.getTotalCount());
    }
}
