package lan.chaos.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lan.chaos.mybatisplus.common.model.OrderUserVO;
import lan.chaos.mybatisplus.common.model.User;
import lan.chaos.mybatisplus.page.PageScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页：单表分页 + 联表分页。
 */
@SpringBootTest
class PageScenarioTest {

    @Autowired
    private PageScenario pageScenario;

    @Test
    void userPage() {
        IPage<User> page = pageScenario.userPage(1, 2);
        assertEquals(2, page.getRecords().size());
        assertEquals(5, page.getTotal());
    }

    @Test
    void orderUserPage() {
        IPage<OrderUserVO> page = pageScenario.orderUserPage(1, 3);
        assertEquals(3, page.getRecords().size());
        assertEquals(6, page.getTotal());
        assertFalse(page.getRecords().isEmpty());
        // 联表结果：userName 来自 t_user，orderTime 来自 t_order
        assertTrue(page.getRecords().stream().allMatch(v -> v.getUserName() != null));
    }
}
