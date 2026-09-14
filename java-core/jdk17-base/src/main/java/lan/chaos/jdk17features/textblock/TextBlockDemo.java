package lan.chaos.jdk17features.textblock;

/**
 * 文本块 Text Blocks（JEP 378，JDK15 定稿，JDK17 用户视角为"新"）：用 {@code """} 表示多行字符串。
 *
 * <p>WHY：以前拼 JSON / SQL / HTML 只能靠字符串拼接与 {@code \n}，可读性极差且缩进混乱。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code """ ..."""} 直接写多行，自动去掉共同的前导缩进（{@code stripIndent()}）；</li>
 *   <li>{@code formatted(Object...)}（JDK15，替代 {@code String.format}）在文本块里做占位替换；</li>
 *   <li>{@code translateEscapes()} 把 {@code \n} 这样的转义序列真正转成换行/制表。</li>
 * </ul>
 * 生产坑点：文本块会保留末尾换行；若不想保留，把结束定界符紧贴内容最后一行。
 */
public class TextBlockDemo {

    public static void run() {
        String json = """
                {
                  "name": "chaos",
                  "age": 18
                }
                """;
        System.out.println("文本块 JSON:\n" + json);

        String greeting = """
                Hello %s, you are %d
                """.formatted("world", 18);
        System.out.println("formatted: " + greeting);

        // translateEscapes：把字面量里的 \n 真正变成换行
        String escaped = "a\\nb\\tc".translateEscapes();
        System.out.println("translateEscapes('a\\\\nb\\\\tc'): [" + escaped + "]");
    }
}
