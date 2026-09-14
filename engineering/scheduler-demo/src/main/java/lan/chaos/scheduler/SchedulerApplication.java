package lan.chaos.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务 Demo 启动类。
 *
 * <p>{@code @EnableScheduling} 开启 {@code @Scheduled}；Quartz 由 spring-boot-starter-quartz
 * 自动配置（内存）；XXL-JOB 执行端仅在配置了 {@code xxl.job.admin.addresses} 时启用。
 *
 * <p>运行后得到「控制台分节输出」：
 * <ul>
 *   <li>@Scheduled：fixedRate/fixedDelay/cron 每 ~1s 打印一次。</li>
 *   <li>QuartzDemo.main 单独跑可看 Quartz 独立调度（见 QuartzDemo）。</li>
 *   <li>XXL-JOB：需在 application.yml 配 admin 地址后才会向 admin 注册。</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
