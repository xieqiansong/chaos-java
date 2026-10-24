package lan.chaos.oauth2.auth.client;

import lan.chaos.oauth2.auth.common.model.SampleClient;
import lan.chaos.oauth2.auth.common.model.Scope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端注册与管理（能力①）。
 *
 * <p>覆盖：注册（自动审核通过，留 {@code requireManualReview} 开关注释生产做法）、
 * 客户端身份验证（bcrypt 比对 secret）、密钥轮换。演示启动时注入两个样例第三方应用。</p>
 */
@Service
public class ClientRegistrationService {

    private final ClientRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public ClientRegistrationService(ClientRepository repository) {
        this.repository = repository;
    }

    /** 启动时 seed 样例客户端：一个保密客户端 + 一个公共(PKCE)客户端。 */
    @PostConstruct
    public void seed() {
        repository.save(SampleClient.sampleThirdPartyApp());
        repository.save(SampleClient.samplePublicClient());
    }

    public Optional<Client> getClient(String clientId) {
        return repository.findByClientId(clientId);
    }

    /**
     * 注册新客户端。演示「审核机制」：这里直接 APPROVED；生产可改成 PENDING 并人工/自动审核。
     * 公开注释：{@code boolean requireManualReview = true;} 时返回待审核状态。
     */
    public ClientRegistrationResult register(ClientRegistrationRequest request) {
        String clientId = "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Client client = new Client();
        client.setClientId(clientId);
        client.setClientName(request.getClientName());
        client.setPkceRequired(request.isPkceRequired());

        String rawSecret = null;
        if (!request.isPkceRequired()) {
            // 保密客户端才发 secret
            rawSecret = generateSecret();
            client.setClientSecretHash(encoder.encode(rawSecret));
        }

        if (request.getRedirectUris() != null) {
            client.getRedirectUris().addAll(request.getRedirectUris());
        }
        if (request.getScopes() != null) {
            for (String s : request.getScopes()) {
                Scope scope = Scope.fromValue(s);
                if (scope != null) {
                    client.getScopes().add(scope);
                }
            }
        }
        if (request.getGrantTypes() != null) {
            client.getGrantTypes().addAll(request.getGrantTypes());
        } else {
            client.getGrantTypes().addAll(Arrays.asList("authorization_code", "refresh_token"));
        }

        repository.save(client);
        return new ClientRegistrationResult(clientId, rawSecret, client.getClientName(),
                new HashSet<>(client.getRedirectUris()),
                scopeValues(client.getScopes()), client.isPkceRequired());
    }

    /** 密钥轮换：生成新 secret 并替换哈希，旧 secret 立即失效。 */
    public ClientRegistrationResult rotateSecret(String clientId) {
        Client client = repository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("client not found: " + clientId));
        if (client.isPkceRequired()) {
            throw new IllegalArgumentException("公共客户端无 secret，无需轮换");
        }
        String rawSecret = generateSecret();
        client.setClientSecretHash(encoder.encode(rawSecret));
        return new ClientRegistrationResult(clientId, rawSecret, client.getClientName(),
                new HashSet<>(client.getRedirectUris()),
                scopeValues(client.getScopes()), client.isPkceRequired());
    }

    /** 客户端身份验证：保密客户端需 secret 匹配；公共客户端（无哈希）仅凭 client_id 识别。 */
    public boolean authenticate(String clientId, String rawSecret) {
        return repository.findByClientId(clientId)
                .map(client -> {
                    if (client.getClientSecretHash() == null) {
                        // 公共客户端：身份即 client_id，安全来自 PKCE
                        return true;
                    }
                    return rawSecret != null
                            && encoder.matches(rawSecret, client.getClientSecretHash());
                })
                .orElse(false);
    }

    private String generateSecret() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Set<String> scopeValues(Set<Scope> scopes) {
        Set<String> values = new HashSet<>();
        for (Scope s : scopes) {
            values.add(s.getValue());
        }
        return values;
    }
}
