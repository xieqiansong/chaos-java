package lan.chaos.redis.common.config;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import lan.chaos.redis.pubsub.PubSubService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Pub/Sub 监听配置（◆ 基础）。
 *
 * <p>订阅 {@code RedisKeyConstants.PUBSUB_CHANNEL} 频道；收到消息后打印控制台，
 * 并交给 {@link PubSubService#record(String)} 记录，供 {@link PubSubService#recent()} 查询。</p>
 */
@Configuration
public class PubSubConfig {

    private final PubSubService pubSubService;

    public PubSubConfig(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory, MessageListener redisMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(redisMessageListener, new ChannelTopic(RedisKeyConstants.PUBSUB_CHANNEL));
        return container;
    }

    @Bean
    public MessageListener redisMessageListener() {
        return (message, pattern) -> {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            System.out.println("[Redis Pub/Sub] channel=" + channel + " body=" + body);
            pubSubService.record(body);
        };
    }
}
