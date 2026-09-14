package lan.chaos.springai.output;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * chat/output：结构化输出场景（让模型直接返回 Java 对象，而不是一段文本）。
 *
 * <p>WHY：业务系统要的是"能直接落库/参与计算"的数据，而不是一段需要正则去抠的自然语言。
 * Spring AI 的做法是 {@code StructuredOutputConverter} 三件套——把"约定格式"塞进提示词，
 * 再把模型的响应反解析回 Java 类型：</p>
 *
 * <ul>
 *   <li>{@link BeanOutputConverter}：绑定 POJO（最常用，适合复杂结构）；</li>
 *   <li>{@link ListOutputConverter}：绑定 {@code List<String>}（简单列表）；</li>
 *   <li>{@code MapOutputConverter}：绑定 Map（结构不固定时）。</li>
 * </ul>
 */
@Service
public class StructuredOutputService {

    private final ChatClient chatClient;

    public StructuredOutputService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Bean 输出：把模型回答直接转成 POJO。
     *
     * @param actor 演员名
     */
    public ActorFilms extractActorFilms(String actor) {
        BeanOutputConverter<ActorFilms> converter = new BeanOutputConverter<>(ActorFilms.class);
        String response = chatClient.prompt()
                .user(u -> u.text("""
                        请列出 {actor} 最著名的 3 部电影，包含片名与上映年份。
                        只输出符合下面格式要求的 JSON，不要任何解释文字，也不要用代码块包裹。
                        {format}
                        """)
                        .param("actor", actor)
                        .param("format", converter.getFormat()))
                .call()
                .content();
        return converter.convert(response);
    }

    /**
     * List 输出：把模型回答转成简单字符串列表。
     *
     * @param text 待提取关键词的文本
     */
    public List<String> extractKeywords(String text) {
        ListOutputConverter converter = new ListOutputConverter();
        String response = chatClient.prompt()
                .user(u -> u.text("""
                        从下面这段文本中提取 5 个关键词。
                        文本：{text}
                        只输出符合下面格式要求的关键词列表，不要任何解释文字。
                        {format}
                        """)
                        .param("text", text)
                        .param("format", converter.getFormat()))
                .call()
                .content();
        return converter.convert(response);
    }
}
