package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.common.model.User;
import lan.chaos.mybatisplus.wrapper.WrapperScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件构造器 Wrapper 高阶用法。
 */
@SpringBootTest
class WrapperScenarioTest {

    @Autowired
    private WrapperScenario wrapperScenario;

    @Test
    void complexQuery() {
        List<User> list = wrapperScenario.complexQuery();
        assertFalse(list.isEmpty());
        // age between 18 and 40 且 name 含 'a'
        assertTrue(list.stream().allMatch(u -> u.getAge() >= 18 && u.getAge() <= 40
                && u.getName().toLowerCase().contains("a")));
    }

    @Test
    void customSqlWithWrapper() {
        List<User> list = wrapperScenario.customSqlWithWrapper();
        assertFalse(list.isEmpty());
        // 仅断言结果全部满足 age > 20（SQL 条件在 wrapper 中拼接生效）
        assertTrue(list.stream().allMatch(u -> u.getAge() > 20));
    }
}
