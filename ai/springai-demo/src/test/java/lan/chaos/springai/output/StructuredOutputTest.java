package lan.chaos.springai.output;

import lan.chaos.springai.testutil.ModelEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/output 结构化输出测试（真实模型）。
 *
 * <p>验证：模型按 Schema 输出后能被正确反序列化为 Java 对象——这是"能落库"的关键。</p>
 */
@SpringBootTest
class StructuredOutputTest {

    @Autowired
    private StructuredOutputService service;

    @Test
    void extractActorFilmsBindsToPojo() {
        ModelEndpoint.assumeUp();

        ActorFilms result = service.extractActorFilms("周星驰");

        System.out.println("=== output/Bean 结构化输出 ===");
        System.out.println("【演员】" + result.actor());
        result.films().forEach(f -> System.out.println("  └ " + f.title() + " (" + f.year() + ")"));

        assertThat(result.actor()).as("演员名应正确绑定").isNotBlank();
        assertThat(result.films()).as("电影列表非空").isNotEmpty();
        assertThat(result.films()).as("返回 3 部电影").hasSize(3);
        assertThat(result.films().get(0).title()).as("片名非空").isNotBlank();
        assertThat(result.films().get(0).year()).as("年份已解析为数字").isNotNull();
    }

    @Test
    void extractKeywordsBindsToList() {
        ModelEndpoint.assumeUp();

        String text = "Spring AI 为 Java 开发者提供了统一的模型抽象层，支持流式输出、工具调用与检索增强生成。";
        List<String> keywords = service.extractKeywords(text);

        System.out.println("=== output/List 结构化输出 ===");
        System.out.println("【关键词】" + keywords);

        assertThat(keywords).as("关键词列表非空").isNotEmpty();
        assertThat(keywords).as("每个关键词非空").allSatisfy(k -> assertThat(k).isNotBlank());
    }
}
