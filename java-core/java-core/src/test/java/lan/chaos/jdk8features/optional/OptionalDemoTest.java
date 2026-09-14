package lan.chaos.jdk8features.optional;

import lan.chaos.jdk8features.common.SampleData;
import lan.chaos.jdk8features.common.model.User;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionalDemoTest {

    @Test
    void safeGetAndFallback() {
        List<User> users = SampleData.sampleUsers();

        String city = users.stream().findFirst().map(User::getCity).orElse("未知");
        assertEquals("北京", city);

        Optional<String> empty = Optional.ofNullable(null);
        assertEquals("默认值(惰性计算)", empty.orElseGet(() -> "默认值(惰性计算)"));

        List<User> none = Collections.emptyList();
        assertEquals("无", none.stream().findFirst().map(User::getName).orElse("无"));
    }
}
