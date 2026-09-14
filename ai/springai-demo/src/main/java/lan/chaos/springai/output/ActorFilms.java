package lan.chaos.springai.output;

import java.util.List;

/**
 * 结构化输出的目标类型：演员代表作（嵌套结构，演示复杂 POJO 的自动绑定）。
 *
 * <p>WHY：用 record 表达"要什么结构"，比手写 JSON 解析更直观；
 * {@code BeanOutputConverter} 会据此生成 JSON Schema 下发给模型，再用 Jackson 反序列化回来。</p>
 */
public record ActorFilms(String actor, List<Film> films) {

    public record Film(String title, Integer year) {
    }
}
