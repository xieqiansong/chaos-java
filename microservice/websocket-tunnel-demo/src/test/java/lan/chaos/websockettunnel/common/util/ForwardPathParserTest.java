package lan.chaos.websockettunnel.common.util;

import lan.chaos.websockettunnel.common.model.TargetAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForwardPathParserTest {

    @Test
    void parsesHostPortAndIgnoresQueryString() {
        TargetAddress target = ForwardPathParser.parse("/forward/127.0.0.1/3306?token=demo-token");

        assertThat(target).isEqualTo(new TargetAddress("127.0.0.1", 3306));
    }

    @Test
    void parsesSampleAddress() {
        TargetAddress sample = TargetAddress.sample();
        assertThat(ForwardPathParser.parse("/forward/" + sample.getHost() + "/" + sample.getPort()))
                .isEqualTo(sample);
    }

    @Test
    void rejectsPathsWithoutForwardPrefix() {
        assertThat(ForwardPathParser.parse("/actuator/health")).isNull();
        assertThat(ForwardPathParser.parse(null)).isNull();
    }

    @Test
    void rejectsMalformedTarget() {
        assertThat(ForwardPathParser.parse("/forward/127.0.0.1")).isNull();
        assertThat(ForwardPathParser.parse("/forward/127.0.0.1/not-a-port")).isNull();
        assertThat(ForwardPathParser.parse("/forward//3306")).isNull();
        assertThat(ForwardPathParser.parse("/forward/127.0.0.1/70000")).isNull();
    }
}
