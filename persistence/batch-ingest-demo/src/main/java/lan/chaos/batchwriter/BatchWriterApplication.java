package lan.chaos.batchwriter;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import lan.chaos.batchwriter.config.BenchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Redis 批量入库引擎 Demo。
 *
 * <p>演示三种写入形态的压测对比：
 * <ul>
 *   <li>legacy：每个小对象即发一次 Redis 写命令（基准，命令数 = 条目数）；</li>
 *   <li>static：定批 + Pipeline（有批量但批量大小固定，不可自适应）；</li>
 *   <li>adaptive：自适应批量引擎（目标方案），批量大小随实时吞吐在线寻优。</li>
 * </ul>
 *
 * <p>压测入口：{@code --batchingest.bench.enabled=true} 启动即跑压力测试、打印结果后退出。
 */
@SpringBootApplication
@EnableConfigurationProperties({BatchWriterProperties.class, BenchProperties.class})
public class BatchWriterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchWriterApplication.class, args);
    }
}