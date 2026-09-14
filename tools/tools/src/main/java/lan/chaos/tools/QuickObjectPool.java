package lan.chaos.tools;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * 对象池工具类
 *
 * @param <T> 对象类型
 */
@Slf4j
public class QuickObjectPool<T> {
    // 对象存储队列
    private final Queue<PooledObject<T>> objectQueue = new ConcurrentLinkedQueue<>();
    // 对象工厂
    private final Supplier<PooledObject<T>> objectFactory;
    // 池的最大容量
    private final int maxSize;

    /**
     * 构造函数
     *
     * @param objectFactory 对象工厂
     * @param maxSize       最大容量
     */
    public QuickObjectPool(Supplier<PooledObject<T>> objectFactory, int maxSize) {
        this.objectFactory = objectFactory;
        this.maxSize = maxSize;
    }

    /**
     * 从对象池获取对象
     * 优先从队列获取匹配版本的对象，获取不到再生成
     *
     * @param targetVersion 目标版本号
     * @return 对象实例
     */
    public T getObject(int targetVersion) {
        PooledObject<T> pooledObj = null;
        for (int i = 0; i < maxSize; i++) {
            pooledObj = objectQueue.poll();
            if (Objects.isNull(pooledObj)) {
                break;
            }
            if (Objects.equals(pooledObj.getVersion(), targetVersion)) {
                break;
            }
        }
        // 队列中没有匹配版本的对象，创建新对象
        if (Objects.isNull(pooledObj)) {
            pooledObj = objectFactory.get();
        }
        // 异步补充对象到队列
        ExecutorService executor = ForkJoinPool.commonPool();
        executor.submit(this::refillQueue);
        return Optional.ofNullable(pooledObj).map(PooledObject::getObject).orElse(null);
    }

    /**
     * 补充队列的实际逻辑
     */
    private void refillQueue() {
        while (objectQueue.size() < maxSize) {
            PooledObject<T> pooledObj = objectFactory.get();
            objectQueue.offer(pooledObj);
        }
    }

    /**
     * 内部类：包装对象和版本号
     */
    @Data
    @AllArgsConstructor
    public static class PooledObject<T> {
        private T object;
        private int version;
    }
}