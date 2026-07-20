package lan.chaos.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lan.chaos.mybatisplus.entity.User;
import lan.chaos.mybatisplus.page.OrderUserVO;
import lan.chaos.mybatisplus.page.PageScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PageScenarioTest {

    @Autowired
    private PageScenario pageScenario;

    @Test
    void userPage() {
        IPage<User> page = pageScenario.userPage(1, 3);
        assertThat(page.getTotal()).isGreaterThan(0);
        assertThat(page.getRecords()).hasSize(3);
    }

    @Test
    void orderUserPage() {
        IPage<OrderUserVO> page = pageScenario.orderUserPage(1, 3);
        assertThat(page.getRecords()).hasSize(3);
        assertThat(page.getRecords()).allMatch(v ->
                v.getUserName() != null && v.getAmount() != null && v.getOrderTime() != null);
    }
}
