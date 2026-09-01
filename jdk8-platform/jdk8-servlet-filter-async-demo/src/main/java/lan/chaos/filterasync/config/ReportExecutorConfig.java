package lan.chaos.filterasync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ReportExecutorConfig {

    /**
     * 独立隔离池：不挤占 {@code ForkJoinPool.commonPool}，也不与其他业务异步任务互相干扰。
     * 队列满时由提交线程自己执行（CallerRunsPolicy），天然背压（降级而非丢弃）。
     */
    @Bean("reportAsyncExecutor")
    public Executor reportAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(65536);
        executor.setThreadNamePrefix("report-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);   // 优雅停机：等队列排空
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
