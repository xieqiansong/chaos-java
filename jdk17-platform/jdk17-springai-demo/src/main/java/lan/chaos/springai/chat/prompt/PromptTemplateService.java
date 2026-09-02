package lan.chaos.springai.chat.prompt;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * chat/prompt：提示词模板场景。
 *
 * <p>WHY：业务提示词往往是"固定骨架 + 动态变量"（如角色、上下文、待处理内容）。
 * {@link PromptTemplate} 用 {@code {name}} 占位符 + 变量 Map 渲染，避免字符串拼接，
 * 提示词模板可独立维护（后续可外置到资源文件）。</p>
 *
 * <p>语法：{@code {变量名}} 占位；也支持 {@code {var: 默认值}} 缺省、{@code {#if}{/if}} 条件分支等高级语法。</p>
 */
@Service
public class PromptTemplateService {

    private final ChatClient chatClient;

    public PromptTemplateService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /** 纯渲染：只做模板变量替换，不调用模型（可单测、可离线调试）。 */
    public String render(String template, Map<String, Object> variables) {
        return new PromptTemplate(template).render(variables);
    }

    /** 模板驱动对话：按模板生成用户消息并调用模型。 */
    public String chat(String template, Map<String, Object> variables) {
        Prompt prompt = new PromptTemplate(template).create(variables);
        return chatClient.prompt(prompt).call().content();
    }
}
