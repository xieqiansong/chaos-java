package lan.chaos.dubbo.api;

import lan.chaos.dubbo.api.common.model.User;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dubbo RPC 契约（接口先行）。
 *
 * <p>WHY：Dubbo 是「面向接口」的 RPC 框架——provider 与 consumer 都只依赖这个接口，
 * 不依赖彼此的实现。接口放在独立模块 {@code dubbo-api} 中，provider 实现它、consumer 引用它，
 * 编译期就解耦，这正是 Dubbo 相比「直接 new 实现」最基础的协作形态。</p>
 *
 * <p>关键约定：
 * <ul>
 *   <li>{@link #getById} / {@link #list} 是同步调用，consumer 拿到的是远程方法返回值；</li>
 *   <li>{@link #getByIdAsync} 返回 {@link CompletableFuture}，Dubbo 据此识别为「异步调用」，
 *        consumer 端无需额外配置即可 {@code .get()} 或 {@code .thenApply(...)}，线程不阻塞在 IO 上。</li>
 * </ul>
 */
public interface UserService {

    /** 按 id 查询单个用户（同步）。 */
    User getById(Long id);

    /** 查询用户列表（同步）。 */
    List<User> list();

    /**
     * 按 id 异步查询单个用户。
     * <p>返回 {@link CompletableFuture} 即声明「本方法异步」：provider 端用
     * {@code CompletableFuture.supplyAsync(...)} 提供结果，Dubbo 在 consumer 端原样透传，
     * 调用方自行决定阻塞 {@code .get()} 还是回调 {@code .thenAccept(...)}。</p>
     */
    CompletableFuture<User> getByIdAsync(Long id);
}
