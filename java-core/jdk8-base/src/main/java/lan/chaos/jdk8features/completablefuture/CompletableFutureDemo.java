package lan.chaos.jdk8features.completablefuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * CompletableFuture（JDK8）：异步编程与组合，告别"回调地狱"（嵌套 Future + 手动 get 阻塞）。
 *
 * <p>WHY：{@code Future.get()} 会阻塞线程且无法组合；CompletableFuture 提供非阻塞的 then* 链式编排。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code supplyAsync} 异步产出结果；{@code thenApply} 转换；{@code thenCombine} 合并两个未来；</li>
 *   <li>{@code thenAccept}/{@code whenComplete} 等非阻塞回调更适合生产；</li>
 *   <li>{@code get()} 会阻塞，演示里用它是为了拿到确定结果做断言（生产慎用）。</li>
 * </ul>
 */
public class CompletableFutureDemo {

    public static void run() {
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 20);
        CompletableFuture<Integer> sum = a.thenCombine(b, Integer::sum);
        try {
            System.out.println("异步计算 10+20=" + sum.get());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        // 链式转换
        CompletableFuture<String> chained = CompletableFuture.supplyAsync(() -> "hello")
                .thenApply(s -> s + " world")
                .thenApply(String::toUpperCase);
        try {
            System.out.println("链式: " + chained.get());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}
