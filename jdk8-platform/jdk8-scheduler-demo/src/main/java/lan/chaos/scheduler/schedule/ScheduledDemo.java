package lan.chaos.scheduler.schedule;

import lan.chaos.scheduler.common.model.JobSample;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * ★★★ 高频：Spring {@code @Scheduled} —— 单进程内最轻量的定时任务方案。
 *
 * <p>痛点：很多「周期性小任务」（缓存预热、心跳上报、临时清理）不值得上分布式调度，
 * 一个注解就能解决。关键 API：{@code @EnableScheduling} + {@code @Scheduled(...)}。
 *
 * <p>三种触发模型（生产最常问的区别）：
 * <ul>
 *   <li>{@code fixedRate}：以上一次<b>开始</b>时间为基准，固定频率触发（任务耗时 > 间隔会重叠）。</li>
 *   <li>{@code fixedDelay}：以上一次<b>结束</b>时间为基准，结束后等固定间隔再触发（绝不重叠）。</li>
 *   <li>{@code cron}：按 cron 表达式精确控制（如每天 0 点、每 5 分钟）。</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>默认单线程执行（{@code SimpleAsyncTaskExecutor} 实际每次新线程，但同一任务串行），
 *       任务耗时长会阻塞后续触发；需配 {@code @Async} 或自定义 {@code TaskScheduler} 线程池。</li>
 *   <li>单进程：多实例部署会<b>重复执行</b>，需要集群去重（即 XXL-JOB 解决的问题）。</li>
 *   <li>{@code fixedRate} 在任务超时时会「追赶式」连续触发，可能打爆下游。</li>
 * </ul>
 */
@Component
@EnableScheduling
public class ScheduledDemo {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /** 手动触发一次：供测试确定性断言，也与真实调度走同一业务逻辑。 */
    public String manualTick(String label, String jobName) {
        counters.computeIfAbsent(label, k -> new AtomicInteger()).incrementAndGet();
        JobSample job = JobSample.sampleJob(jobName);
        String out = job.describe();
        System.out.printf("[scheduled/%s] 计数=%d | %s%n", label, counters.get(label).get(), out);
        return out;
    }

    /** 供测试观察「调度器确实在后台触发」。 */
    public int countOf(String label) {
        AtomicInteger c = counters.get(label);
        return c == null ? 0 : c.get();
    }

    @Scheduled(fixedRate = 1000)
    public void fixedRateTask() {
        manualTick("fixedRate", "heartbeat");
    }

    @Scheduled(fixedDelay = 1000)
    public void fixedDelayTask() {
        manualTick("fixedDelay", "cache-warmup");
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void cronTask() {
        manualTick("cron", "metrics-report");
    }
}
