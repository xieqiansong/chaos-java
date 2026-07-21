package lan.chaos.jdk8features.stringjoiner;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringJoinerDemoTest {

    @Test
    void joinVariants() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        Arrays.asList("北京", "上海", "广州").forEach(sj::add);
        assertEquals("[北京, 上海, 广州]", sj.toString());

        assertEquals("北京 | 上海 | 广州", String.join(" | ", Arrays.asList("北京", "上海", "广州")));

        String merged = Arrays.asList("北京", "上海", "广州").stream()
                .collect(Collectors.joining("-", "<", ">"));
        assertEquals("<北京-上海-广州>", merged);
    }
}
