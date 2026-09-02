package lan.chaos.seata;

import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SAGA 官方状态机引擎集成测试（需要真实 Seata Server，否则 Assumptions 跳过）。
 *
 * <p>与 {@link SagaScenarioTest}（手写补偿链，纯本地）互补：本测试驱动 Seata 官方
 * <b>seata-saga-statelang</b> 引擎，由 JSON 状态机（{@code saga/stock_purchase.json}）
 * 编排正向扣款 → 建单 → 扣库存，失败时自动按逆序执行 CompensateState 补偿。</p>
 *
 * <p>运行方式（需先启动 docker-compose 的 seata-server）：</p>
 * <pre>
 *   docker compose -f jdk8-seata-demo/docker-compose.yml up -d
 *   mvn -pl jdk8-seata-demo -am test -Dseata.enabled=true
 * </pre>
 *
 * <p>引擎 Bean（{@code SagaEngineConfig}）仅在 {@code seata.enabled=true} 时装配，
 * 因此这里用 {@code @Autowired(required = false)} + 门控跳过，避免单测环境上下文加载失败。</p>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("SAGA 官方状态机引擎集成测试")
class SagaEngineIntegrationTest {

    /** 与 saga/stock_purchase.json 中 Name 保持一致 */
    private static final String STATE_MACHINE_NAME = "stockPurchaseSaga";

    @Autowired(required = false)
    private StateMachineEngine stateMachineEngine;
    @Autowired
    private Environment environment;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void requireRealSeataServerAndEngine() {
        boolean seataEnabled = Boolean.parseBoolean(environment.getProperty("seata.enabled", "false"));
        Assumptions.assumeTrue(seataEnabled && stateMachineEngine != null,
                "需真实 Seata Server + 官方 SAGA 引擎（docker-compose + -Dseata.enabled=true 时运行）");
    }

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM order_tbl");
        jdbcTemplate.update("DELETE FROM undo_log");
        jdbcTemplate.update("DELETE FROM seata_state_inst");
        jdbcTemplate.update("DELETE FROM seata_state_machine_inst");
        jdbcTemplate.update("MERGE INTO account (user_id, balance, frozen) KEY(user_id) VALUES (?, ?, 0)",
                USER_ID, INIT_BALANCE);
        jdbcTemplate.update("MERGE INTO storage (product_id, total, frozen) KEY(product_id) VALUES (?, ?, 0)",
                PRODUCT_ID, INIT_STOCK);
    }

    @Test
    @DisplayName("SAGA-engine-1: 状态机定义启动时自动注册到 seata_state_machine_def")
    void stateMachineDefAutoRegistered() {
        Integer defs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seata_state_machine_def WHERE name = ?",
                Integer.class, STATE_MACHINE_NAME);
        assertEquals(1, defs, "引擎启动时应把 saga/*.json 解析并写入状态机定义表");
    }

    @Test
    @DisplayName("SAGA-engine-2: 官方引擎正向执行 — 扣款→建单→扣库存全部成功")
    void engineForwardSuccess() {
        StateMachineInstance inst = stateMachineEngine.start(STATE_MACHINE_NAME, null, startParams());

        assertEquals(ExecutionStatus.SU, inst.getStatus(), "状态机实例应执行成功");
        assertEquals(INIT_BALANCE - ORDER_AMOUNT, balance(), 0.01, "余额扣减");
        assertEquals(INIT_STOCK - ORDER_COUNT, stock(), "库存扣减");
        assertEquals(1, orderCount(), "创建一条订单");
    }

    @Test
    @DisplayName("SAGA-engine-3: 扣库存失败触发官方引擎逆序补偿 — 撤销订单 + 加回余额")
    void engineInsufficientStockCompensates() {
        Map<String, Object> params = startParams();
        params.put("count", INIT_STOCK + 999); // 第三步扣库存必然失败

        StateMachineInstance inst = stateMachineEngine.start(STATE_MACHINE_NAME, null, params);

        assertEquals(ExecutionStatus.FA, inst.getStatus(), "状态机实例应标记失败");
        // 引擎按逆序调用 CompensateState：撤销订单 → 加回余额
        assertEquals(INIT_BALANCE, balance(), 0.01, "补偿后余额恢复");
        assertEquals(INIT_STOCK, stock(), "库存不变（扣减失败）");
        assertEquals(0, orderCount(), "补偿后订单被撤销");
    }

    private Map<String, Object> startParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", USER_ID);
        params.put("productId", PRODUCT_ID);
        params.put("amount", ORDER_AMOUNT);
        params.put("count", ORDER_COUNT);
        params.put("orderNo", UUID.randomUUID().toString().replace("-", ""));
        return params;
    }

    private double balance() {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?", Double.class, USER_ID);
    }

    private int stock() {
        return jdbcTemplate.queryForObject(
                "SELECT total FROM storage WHERE product_id = ?", Integer.class, PRODUCT_ID);
    }

    private int orderCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_tbl", Integer.class);
    }
}
