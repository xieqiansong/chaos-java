package lan.chaos.scheduler.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quartz 的 Job 实现：被调度器在触发时实例化并执行 {@link #execute}。
 *
 * <p>用静态计数器统计执行次数，便于测试断言「确实被调度触发了 N 次」。
 * 真实场景里 Job 通常无状态，状态放在 {@link JobExecutionContext#getJobDetail()} 的 JobDataMap 中。
 */
public class SampleQuartzJob implements Job {

    /** 跨实例共享的执行计数（演示用，单测前会 reset）。 */
    public static final AtomicInteger EXECUTIONS = new AtomicInteger(0);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int n = EXECUTIONS.incrementAndGet();
        String name = context.getJobDetail().getKey().getName();
        System.out.printf("[quartz] 第 %d 次执行 job=%s%n", n, name);
    }
}
