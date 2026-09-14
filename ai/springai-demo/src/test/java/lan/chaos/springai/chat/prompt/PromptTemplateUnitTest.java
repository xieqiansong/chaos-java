package lan.chaos.springai.chat.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/prompt 纯单元测试：模板渲染语义（不依赖模型/外部服务）。
 *
 * <p>渲染是纯函数，是提示词工程中最值得单测的部分。注意 Spring AI 1.1 模板引擎为 ST4：
 * 变量占位符 {@code {name}}，条件分支 {@code {if(cond)}...{endif}}。</p>
 */
class PromptTemplateUnitTest {

    @Test
    void renderReplacesVariables() {
        String rendered = new PromptTemplate("我是{name}，来自{company}。")
                .render(Map.of("name", "小明", "company", "腾讯"));
        assertThat(rendered).isEqualTo("我是小明，来自腾讯。");
    }

    @Test
    void renderSupportsConditionalBranch() {
        String template = "{if(debug)}输出调试信息。{endif}执行任务：{task}";

        String withDebug = new PromptTemplate(template).render(Map.of("debug", true, "task", "测试"));
        assertThat(withDebug).isEqualTo("输出调试信息。执行任务：测试");

        String withoutDebug = new PromptTemplate(template).render(Map.of("debug", false, "task", "测试"));
        assertThat(withoutDebug).isEqualTo("执行任务：测试");
    }
}
