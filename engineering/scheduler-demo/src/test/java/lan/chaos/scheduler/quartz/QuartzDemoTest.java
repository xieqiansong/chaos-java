package lan.chaos.scheduler.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quartz 验证：使用内存调度器，固定间隔 1s，观察窗口 2.3s 内应触发 >= 2 次。
 * 零外部依赖（RAMJobStore）。
 */
class QuartzDemoTest {

    @Test
    void inMemorySchedulerFiresRepeatedly() throws SchedulerException {
        int count = QuartzDemo.demoCountAfterWindow();
        assertTrue(count >= 2,
                "Quartz 在 2.3s 窗口内（间隔 1s）应至少触发 2 次，实际=" + count);
    }
}
