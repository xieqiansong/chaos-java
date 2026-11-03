package lan.chaos.dubbo.consumer;

import lan.chaos.dubbo.api.common.model.User;
import lan.chaos.dubbo.consumer.demo.DemoApplication;
import lan.chaos.dubbo.consumer.demo.facade.AsyncUserFacade;
import lan.chaos.dubbo.consumer.demo.facade.BasicUserFacade;
import lan.chaos.dubbo.consumer.demo.facade.ClusterFaultTolerantFacade;
import lan.chaos.dubbo.consumer.demo.facade.LoadBalanceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dubbo 核心机制验证（单进程点对点直连，零外部依赖，CI 必跑）。
 *
 * <p>用「同一 JVM 内 provider 导出 + consumer 直连」的方式，把 Dubbo 的四个关键机制逐一断言：
 * 基础同步调用 / 异步调用 / 集群容错（failover 重试）/ 负载均衡（roundrobin）。
 * 不依赖 Nacos，任何环境直接过。</p>
 */
@SpringBootTest(classes = DemoApplication.class)
class DubboScenarioTest {

    @Autowired
    private BasicUserFacade basic;

    @Autowired
    private AsyncUserFacade async;

    @Autowired
    private ClusterFaultTolerantFacade cluster;

    @Autowired
    private LoadBalanceFacade loadBalance;

    @Test
    void basic_syncCall_returnsSampleUser() {
        User u = basic.getById(1L);
        assertNotNull(u);
        assertEquals(1L, u.getId());
        assertEquals("direct-basic", u.getInstanceId());
    }

    @Test
    void async_call_returnsFutureResult() throws Exception {
        User u = async.getByIdAsync(1L).get();
        assertNotNull(u);
        assertEquals(1L, u.getId());
        assertEquals("direct-async", u.getInstanceId());
    }

    @Test
    void cluster_failover_retriesThenRecovers() {
        // FlakyProvider 前 2 次抛异常，failover(retries=2) 重试后第 3 次成功；
        // age 字段承载「累计尝试次数」，断言为 3，证明确实发生了重试并恢复。
        User u = cluster.getById(1L);
        assertNotNull(u);
        assertEquals(3, u.getAge(), "failover 应重试至第 3 次成功");
    }

    @Test
    void loadBalance_roundrobin_hitsBothInstances() {
        List<String> hits = loadBalance.whichInstances(10);
        assertEquals(10, hits.size());
        assertTrue(hits.contains("A"), "roundrobin 应命中实例 A");
        assertTrue(hits.contains("B"), "roundrobin 应命中实例 B");
    }
}
