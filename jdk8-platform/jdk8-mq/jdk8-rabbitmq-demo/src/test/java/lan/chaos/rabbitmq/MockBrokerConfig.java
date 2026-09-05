package lan.chaos.rabbitmq;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 自包含测试用内存 Broker：用 rabbitmq-mock 替换底层 ConnectionFactory。
 *
 * <p>类比 Kafka 的 {@code @EmbeddedKafka}——无需 Docker、无需外部 Broker，{@code mvn test} 即绿。
 * 仅由 {@code RabbitmqMockScenarioTest} 通过 {@code @Import} 引入，覆盖 Spring Boot 默认的
 * 真实 ConnectionFactory。内存 Broker 支持 Exchange/Queue/Binding 声明与基础收发，
 * 故 Exchange 路由相关场景可在此完整验证。</p>
 *
 * <p><b>关于消费（ack）：</b>rabbitmq-mock 的 {@code basicGet} 不会在 {@code autoAck=true} 时自动删除消息，
 * 必须显式 {@code basicAck}。Spring 的 {@link org.springframework.amqp.rabbit.core.RabbitTemplate#receive}
 * 仅在通道为事务型时才会回发 {@code basicAck}，因此 {@code RabbitConfig} 中统一把 {@code RabbitTemplate}
 * 设为 {@code channelTransacted=true}（仅 {@code mock} profile），使自包含测试里的「拉取即消费」语义成立
 * （可断言「每条消息只收到一份」）。该设置不影响 *IT / 真实 Broker 的 Publisher Confirm 路径。</p>
 */
@TestConfiguration
public class MockBrokerConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(new MockConnectionFactory());
    }
}
