package lan.chaos.websockettunnel.common.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TargetPolicyTest {

    @Test
    void allowsExactHostPortRule() {
        TargetPolicy policy = new TargetPolicy(Collections.singletonList(" 127.0.0.1:3306 "));

        assertThat(policy.isAllowed("127.0.0.1", 3306)).isTrue();
        assertThat(policy.isAllowed("127.0.0.1", 6379)).isFalse();
    }

    @Test
    void allowsHostWideRuleAndIsCaseInsensitive() {
        TargetPolicy policy = new TargetPolicy(Arrays.asList("LOCALHOST", "192.168.1.10:8080"));

        assertThat(policy.isAllowed("localhost", 12345)).isTrue();
        assertThat(policy.isAllowed("192.168.1.10", 8080)).isTrue();
        assertThat(policy.isAllowed("192.168.1.10", 8081)).isFalse();
    }

    @Test
    void failsClosedWhenNoRuleConfigured() {
        TargetPolicy policy = new TargetPolicy(Collections.emptyList());

        assertThat(policy.isAllowed("127.0.0.1", 3306)).isFalse();
        assertThat(policy.isAllowed(null, 3306)).isFalse();
    }
}
