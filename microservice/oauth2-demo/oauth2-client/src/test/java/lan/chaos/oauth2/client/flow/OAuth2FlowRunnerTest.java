package lan.chaos.oauth2.client.flow;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;

import static org.assertj.core.api.Assertions.assertThat;

/** 轻量测试：仅校验授权端点 URL 组装正确（无需启动三进程）。 */
class OAuth2FlowRunnerTest {

    @Test
    void buildAuthorizeUrl_containsRequiredParams() {
        OAuth2FlowRunner runner = new OAuth2FlowRunner();
        String url = runner.buildAuthorizeUrl("http://localhost:18081", "demo_client_01",
                "http://127.0.0.1:18083/cb", "user.basic", "abc123");

        UriComponents c = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(url).build();
        assertThat(c.getQueryParams().getFirst("client_id")).isEqualTo("demo_client_01");
        assertThat(c.getQueryParams().getFirst("redirect_uri")).isEqualTo("http://127.0.0.1:18083/cb");
        assertThat(c.getQueryParams().getFirst("scope")).isEqualTo("user.basic");
        assertThat(c.getQueryParams().getFirst("state")).isEqualTo("abc123");
        assertThat(c.getQueryParams().getFirst("response_mode")).isEqualTo("json");
        assertThat(c.getQueryParams().getFirst("auto_consent")).isEqualTo("true");
    }
}
