package lan.chaos.jdk8features.stringjoiner;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * StringJoiner / String.join（JDK8）：更优雅地拼接带分隔符、前缀、后缀的字符串，替代 StringBuilder 手工拼。
 *
 * <p>WHY：以前拼 "a, b, c" 要在循环里判断是不是最后一个元素、手动处理逗号，易错又啰嗦。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code new StringJoiner(delimiter, prefix, suffix)} 可带前后缀；</li>
 *   <li>{@code String.join(delimiter, iterable)} 一行搞定简单拼接；</li>
 *   <li>{@code Collectors.joining(...)} 在 Stream 收尾时直接拼接。</li>
 * </ul>
 */
public class StringJoinerDemo {

    public static void run() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        Arrays.asList("北京", "上海", "广州").forEach(sj::add);
        System.out.println("StringJoiner: " + sj);

        List<String> cities = Arrays.asList("北京", "上海", "广州");
        System.out.println("String.join: " + String.join(" | ", cities));

        String merged = cities.stream().collect(Collectors.joining("-", "<", ">"));
        System.out.println("Collectors.joining: " + merged);
    }
}
