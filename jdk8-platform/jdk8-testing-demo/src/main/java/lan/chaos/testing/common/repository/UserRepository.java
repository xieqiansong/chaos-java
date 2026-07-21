package lan.chaos.testing.common.repository;

import lan.chaos.testing.common.model.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口 — 模拟的数据访问层。
 *
 * <p>不需要实现类，测试中通过 Mockito 创建 mock 对象。
 * 这样演示了真实的「依赖外部接口，测试时 mock 掉」场景。
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    List<User> findAll();

    User save(User user);

    void deleteById(Long id);

    long count();
}
