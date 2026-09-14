package lan.chaos.springai.output;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/output 纯单元测试：Converter 本身的解析语义（不依赖模型/外部服务）。
 *
 * <p>WHY：Converter 是纯函数，解析规则（去代码块、类型绑定）值得单独固化——
 * 这样线上出"解析失败"问题时，能快速定位是模型没按格式输出，还是 Converter 的问题。</p>
 */
class StructuredOutputConverterUnitTest {

    @Test
    void beanConverterParsesJson() {
        BeanOutputConverter<ActorFilms> converter = new BeanOutputConverter<>(ActorFilms.class);

        String json = """
                {"actor":"周星驰","films":[{"title":"喜剧之王","year":1999},{"title":"功夫","year":2004}]}
                """;

        ActorFilms result = converter.convert(json);
        assertThat(result.actor()).isEqualTo("周星驰");
        assertThat(result.films()).hasSize(2);
        assertThat(result.films().get(0)).isEqualTo(new ActorFilms.Film("喜剧之王", 1999));
    }

    @Test
    void beanConverterHandlesCodeBlockWrapper() {
        BeanOutputConverter<ActorFilms> converter = new BeanOutputConverter<>(ActorFilms.class);

        // 模型常返回被 ```json 包裹的内容，Converter 应能正确剥离
        String wrapped = "```json\n{\"actor\":\"周星驰\",\"films\":[{\"title\":\"国产凌凌漆\",\"year\":1994}]}\n```";
        ActorFilms result = converter.convert(wrapped);

        assertThat(result.actor()).isEqualTo("周星驰");
        assertThat(result.films()).hasSize(1);
    }

    @Test
    void listConverterParsesCommaSeparatedValues() {
        ListOutputConverter converter = new ListOutputConverter();
        assertThat(converter.convert("Java, Spring, AI")).containsExactly("Java", "Spring", "AI");
    }

    @Test
    void beanConverterExposesJsonSchema() {
        BeanOutputConverter<ActorFilms> converter = new BeanOutputConverter<>(ActorFilms.class);
        String schema = converter.getJsonSchema();

        assertThat(schema).as("应生成 JSON Schema 供模型遵循").contains("actor", "films");
        System.out.println("生成的 JSON Schema：" + schema);
    }
}
