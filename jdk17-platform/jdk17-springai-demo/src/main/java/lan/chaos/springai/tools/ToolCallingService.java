package lan.chaos.springai.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * chat/tools：工具调用场景（Function Calling）。
 *
 * <p>WHY：模型自身无法获取实时数据（时间、天气、订单状态），也无法操作外部系统。
 * 工具调用让模型在"需要时"主动回调业务方法，把 LLM 从"聊天"变成"能干活的智能体"。</p>
 *
 * <p>执行链路：模型判断要调工具 → 框架解析出工具名与参数 → 调用本地方法 →
 * 把结果回填给模型 → 模型组织最终回答。整个过程由 Spring AI 自动驱动，业务只写工具方法。</p>
 */
@Service
public class ToolCallingService {

    private final ChatClient chatClient;

    public ToolCallingService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultTools(new DateTimeTools())
                .build();
    }

    /** 对话：模型可按需自动调用已注册的工具。 */
    public String chat(String userMessage) {
        return chatClient.prompt(userMessage).call().content();
    }

    /** 返回完整响应，便于观察工具调用情况（测试与排障用）。 */
    public ChatResponse chatResponse(String userMessage) {
        return chatClient.prompt(userMessage).call().chatResponse();
    }
}
