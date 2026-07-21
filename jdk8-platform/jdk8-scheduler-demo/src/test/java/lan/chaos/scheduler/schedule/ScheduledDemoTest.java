package lan.chaos.scheduler.schedule;

import lan.chaos.scheduler.common.model.JobSample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Scheduled 验证：
 * 1) 业务逻辑（manualTick）确定性可断言（输入→输出、计数递增）。
 * 2) 调度器确实在后台触发（等待窗口后计数 >= 2）。
 */
@SpringBootTest
class ScheduledDemoTest {

    @Autowired
    private ScheduledDemo scheduledDemo;

    @Test
    void manualTick_producesInputOutputAndCounts() {
        String out1 = scheduledDemo.manualTick("unit", "heartbeat");
        String out2 = scheduledDemo.manualTick("unit", "heartbeat");

        assertTrue(out1.contains("heartbeat"), "输出应包含任务名（输入）");
        assertTrue(out1.contains("已执行"), "输出应体现执行结果（输出）");
        assertEquals(2, scheduledDemo.countOf("unit"), "两次手动触发计数应为 2");
    }

    @Test
    void schedulerActuallyFiresInBackground() throws InterruptedException {
        // 等待 2.3s：fixedRate(1s)/fixedDelay(1s)/cron(每1s) 都应触发至少 2 次
        Thread.sleep(2300);

        assertTrue(scheduledDemo.countOf("fixedRate") >= 2,
                "@Scheduled fixedRate 应在后台触发 >= 2 次");
        assertTrue(scheduledDemo.countOf("fixedDelay") >= 2,
                "@Scheduled fixedDelay 应在后台触发 >= 2 次");
        assertTrue(scheduledDemo.countOf("cron") >= 2,
                "@Scheduled cron 应在后台触发 >= 2 次");
    }
}
