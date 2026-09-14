package lan.chaos.nacos.config;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 在 Spring 上下文启动之前探测 Nacos Server（默认 {@code REDACTED:8848}）是否可达。
 * <p>不可达时整类测试被禁用（优雅跳过），避免 {@code @SpringBootTest} 因 bootstrap 阶段连不上
 * Nacos 而崩溃报错——这是强依赖外部中间件的合规形态（同 rocketmq 的 BrokerReachableCondition）。</p>
 */
public class NacosReachableCondition implements ExecutionCondition {

    private static final String NACOS_HOST = "REDACTED";
    private static final int NACOS_PORT = 8848;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isReachable(NACOS_HOST, NACOS_PORT)) {
            return ConditionEvaluationResult.enabled("Nacos reachable at " + NACOS_HOST + ":" + NACOS_PORT);
        }
        return ConditionEvaluationResult.disabled(
                "Nacos 不可达（" + NACOS_HOST + ":" + NACOS_PORT + "），跳过测试（CI 无外部依赖时零误报）");
    }

    private static boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
