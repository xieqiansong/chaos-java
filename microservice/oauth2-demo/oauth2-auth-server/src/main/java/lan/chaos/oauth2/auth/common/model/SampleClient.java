package lan.chaos.oauth2.auth.common.model;

import lan.chaos.oauth2.auth.client.Client;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * 样例第三方应用工厂。演示两类典型客户端：
 * 1) 保密客户端（有后端，持有 client_secret，可用授权码 + 刷新令牌）。
 * 2) 公共客户端（SPA/移动端，无 secret，必须走 PKCE）。
 *
 * <p>注意：raw secret 仅在此 DEMO 中明文暴露，便于测试与 Runner 直接使用；
 * 生产环境 secret 只以 bcrypt 哈希落库，绝不明文存储或返回。</p>
 */
public final class SampleClient {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private SampleClient() {
    }

    public static final String DEMO_CLIENT_ID = "demo_client_01";
    public static final String DEMO_CLIENT_SECRET = "demo_secret_01";
    public static final String PUBLIC_CLIENT_ID = "demo_public_client_01";

    /** 保密客户端：带 client_secret，支持授权码 + 刷新。 */
    public static Client sampleThirdPartyApp() {
        Client client = new Client();
        client.setClientId(DEMO_CLIENT_ID);
        client.setClientSecretHash(ENCODER.encode(DEMO_CLIENT_SECRET));
        client.setClientName("示例第三方应用（保密客户端）");
        client.getRedirectUris().add("http://127.0.0.1:18083/cb");
        client.getScopes().addAll(EnumSet.of(Scope.USER_BASIC, Scope.USER_EMAIL));
        client.getGrantTypes().addAll(Arrays.asList("authorization_code", "refresh_token"));
        client.setPkceRequired(false);
        return client;
    }

    /** 公共客户端：无 secret，强制 PKCE。 */
    public static Client samplePublicClient() {
        Client client = new Client();
        client.setClientId(PUBLIC_CLIENT_ID);
        client.setClientSecretHash(null);
        client.setClientName("示例公共客户端（SPA/移动端，PKCE）");
        client.getRedirectUris().add("http://127.0.0.1:18083/cb");
        client.getScopes().addAll(EnumSet.of(Scope.USER_BASIC));
        client.getGrantTypes().addAll(Arrays.asList("authorization_code"));
        client.setPkceRequired(true);
        return client;
    }
}
