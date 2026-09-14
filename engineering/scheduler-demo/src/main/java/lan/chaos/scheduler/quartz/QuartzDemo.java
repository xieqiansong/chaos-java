package lan.chaos.scheduler.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.concurrent.TimeUnit;

/**
 * ★★★ 高频：Quartz —— 功能完备的 Java 调度框架，支持持久化 JobStore、
 * Cron、 misfire 策略、集群（JDBC JobStore）。
 *
 * <p>痛点：{@code @Scheduled} 不够灵活（不能动态增删任务、不能持久化、错过触发无策略）。
 * Quartz 用「Job（做什么）+ Trigger（何时做）」解耦，可运行时增删改调度。
 *
 * <p>本 demo 用<b>内存 RAMJobStore</b>（零外部依赖）演示核心机制；生产要持久化/集群时
 * 换成 {@code org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX} + 数据库。
 *
 * <p>关键 API：{@code SchedulerFactory} → {@code Scheduler.scheduleJob(JobDetail, Trigger)} →
 * {@code start()}。{@code SimpleScheduleBuilder} 做固定间隔，{@code CronScheduleBuilder} 做 cron。
 *
 * <p>生产坑：
 * <ul>
 *   <li>misfire（错过触发）策略必须配置，否则任务堆积/不触发行为难预期。</li>
 *   <li>默认线程池 10，长任务会占满线程导致其他任务饿死，需调 {@code org.quartz.threadPool.threadCount}。</li>
 *   <li>集群需共用数据库 + 同一 {@code instanceName}，否则重复触发。</li>
 * </ul>
 */
public class QuartzDemo {

    /** 用内存调度器跑一次演示，返回观察窗口内的执行次数。 */
    public static int runDemo(long waitMillis) throws SchedulerException {
        SampleQuartzJob.EXECUTIONS.set(0);

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDetail job = JobBuilder.newJob(SampleQuartzJob.class)
                .withIdentity("sampleJob", "demo")
                .usingJobData("biz", "cleanup")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("sampleTrigger", "demo")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(1000)
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();

        scheduler.scheduleJob(job, trigger);

        try {
            TimeUnit.MILLISECONDS.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduler.shutdown(true);
        return SampleQuartzJob.EXECUTIONS.get();
    }

    /** 供测试调用：固定等待 2.3s 并返回执行次数。 */
    public static int demoCountAfterWindow() throws SchedulerException {
        return runDemo(2300);
    }
}
