package lan.chaos.flink.cdc.sync;

import cn.hutool.core.util.RandomUtil;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class CheckpointConfig {
    private final static long DEFAULT_CHECKPOINT_INTERVAL = 60;

    public static void defaultCheckpoint(StreamExecutionEnvironment env, String jobName, long intervalSeconds) {
        //开启ck
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        String ckPath = ConfigurationManager.getProperty("flink.ck-path") + "/" + jobName;
        env.getCheckpointConfig().setCheckpointStorage(new FileSystemCheckpointStorage(ckPath));
        env.enableCheckpointing(intervalSeconds * 1000);

        /* ---------------------------------------------------------------------
         * 按照指数方式进行重启
         * 当前设置：
         * 1. 10秒重启，按照1.2的倍率指数增大时间，直到到5分钟
         * 2. 当重启周期执行超过30分钟后，恢复到初始状态
         * ------------------------------------------------------------------ */
        env.setRestartStrategy(RestartStrategies.exponentialDelayRestart(
                //初始状态 10秒重启
                Time.of(10, TimeUnit.SECONDS),
                //最大延迟周期 5分钟
                Time.of(5, TimeUnit.MINUTES),
                //每次增大的延迟倍率 1.2
                1.2,
                // 最到30分钟以后恢复到初始状态
                Time.of(30, TimeUnit.MINUTES),
                // 随机加权策略
                RandomUtil.randomDouble()
        ));
        // 每10秒重启一次，最多10次
        // env.setRestartStrategy(RestartStrategies.fixedDelayRestart(10, org.apache.flink.api.common.time.Time.seconds(10)));
        // 作业停止后CheckPoint数据默认会自动删除，设置在作业失败被取消后CheckPoint数据不被删除
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
    }

    public static void defaultCheckpoint(StreamExecutionEnvironment env, String jobName) {
        defaultCheckpoint(env, jobName, DEFAULT_CHECKPOINT_INTERVAL);
    }
}
