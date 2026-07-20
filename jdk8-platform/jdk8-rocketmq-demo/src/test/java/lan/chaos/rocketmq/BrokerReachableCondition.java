package lan.chaos.rocketmq;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * JUnit 执行条件：仅当 RocketMQ NameServer 可达时才执行 {@code DemoTest}。
 * <p>
 * 必须放在 Spring 上下文启动<b>之前</b>拦截：RocketMQ 的 {@code @RocketMQMessageListener}
 * 在 Bean 初始化阶段就会同步连接 NameServer，不可达会直接导致 ApplicationContext 加载失败
 * （整个测试类 error，而非跳过）。本条件在上下文加载前判定，不可达时整类被禁用，
 * 满足 AGENTS 规范「无外部依赖时靠条件优雅跳过（零误报）」。
 */
public class BrokerReachableCondition implements ExecutionCondition {

    static final String NAMESRV = System.getProperty("rocketmq.namesrv.addr", "REDACTED:9876");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (reachable()) {
            return ConditionEvaluationResult.enabled("RocketMQ NameServer 可达: " + NAMESRV);
        }
        return ConditionEvaluationResult.disabled(
                "RocketMQ NameServer 不可达(" + NAMESRV + ")，跳过（请先 `docker-compose up -d` 起 broker）");
    }

    /** TCP 探测 NameServer 是否可达（1s 超时） */
    static boolean reachable() {
        String[] hp = NAMESRV.split(":");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(hp[0], Integer.parseInt(hp[1])), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
