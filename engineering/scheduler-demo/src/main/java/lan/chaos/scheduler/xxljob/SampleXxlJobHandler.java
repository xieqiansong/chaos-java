package lan.chaos.scheduler.xxljob;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 任务处理器：方法上加 {@link XxlJob} 注解即注册为一个可被 admin 调度的任务。
 *
 * <p>{@code value} 是任务名（admin 端创建任务时填这个），{@code shardIndex/total} 用于分片。
 * 演示一个「分片处理」任务：根据分片下标只处理属于自己那片的数据，避免多实例重复。
 */
@Component
public class SampleXxlJobHandler {

    @XxlJob("schedulerDemoShardJob")
    public void shardJob() {
        // 分片参数由执行端注入到 JobContext
        int shardIndex = com.xxl.job.core.context.XxlJobHelper.getShardIndex();
        int shardTotal = com.xxl.job.core.context.XxlJobHelper.getShardTotal();
        String param = com.xxl.job.core.context.XxlJobHelper.getJobParam();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("分片[%d/%d] 收到参数=%s | ", shardIndex, shardTotal, param));
        // 模拟只处理本分片数据：如 id % shardTotal == shardIndex
        int handled = 0;
        for (int id = 0; id < 100; id++) {
            if (id % shardTotal == shardIndex) handled++;
        }
        sb.append("本分片处理条数=").append(handled);
        System.out.println("[xxl-job] " + sb);
        com.xxl.job.core.context.XxlJobHelper.log(sb.toString());
    }

    @XxlJob("schedulerDemoSimpleJob")
    public void simpleJob() {
        String out = lan.chaos.scheduler.common.model.JobSample
                .sampleJob("xxl-simple").describe();
        System.out.println("[xxl-job] " + out);
        com.xxl.job.core.context.XxlJobHelper.log(out);
    }
}
