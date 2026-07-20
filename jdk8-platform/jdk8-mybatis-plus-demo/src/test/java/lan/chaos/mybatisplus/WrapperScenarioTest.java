package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.entity.User;
import lan.chaos.mybatisplus.wrapper.WrapperScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WrapperScenarioTest {

    @Autowired
    private WrapperScenario wrapperScenario;

    @Test
    void complexQuery() {
        List<User> list = wrapperScenario.complexQuery();
        assertThat(list).isNotEmpty();
        // 验证 Wrapper 的 between / like / 嵌套 OR 生效
        assertThat(list).allMatch(u -> u.getAge() >= 18 && u.getAge() <= 40);
        assertThat(list).allMatch(u -> u.getName() != null && u.getName().toLowerCase().contains("a"));
    }

    @Test
    void customSqlWithWrapper() {
        List<User> list = wrapperScenario.customSqlWithWrapper();
        assertThat(list).isNotEmpty();
        assertThat(list).allMatch(u -> u.getAge() > 20);
    }
}
