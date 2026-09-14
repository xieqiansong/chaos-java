package lan.chaos.springai.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公共 ChatClient 配置：为所有场景提供默认行为，避免每个场景重复配置。
 *
 * <p>WHY：Spring AI 自动装配已提供 {@link ChatClient.Builder}，此处基于它构造默认 ChatClient——
 * 统一注入默认系统提示词（角色设定），各场景类直接注入使用，只需关注自身 prompt。</p>
 */
@Configuration
public class ChatClientConfig {

    /** 默认系统提示词：让模型回答简洁、结构化，后续场景可覆盖。 */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个严谨、简洁的 Java 助手。回答要直接命中要点，不要啰嗦；
            涉及代码时给出可直接运行的片段，并标注关键点。
            """;

    @Bean
    public ChatClient defaultChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }
}
