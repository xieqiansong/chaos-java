package lan.chaos.oauth2.auth.common.util;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 从请求中提取「客户端身份」：优先 Basic 头（client_id:client_secret），
 * 否则回退到表单参数 client_id / client_secret（OAuth2 均允许）。
 */
public final class ClientAuthUtil {

    private ClientAuthUtil() {
    }

    public static ClientCredentials extract(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Basic ")) {
            byte[] decoded = Base64.getDecoder().decode(header.substring(6));
            String pair = new String(decoded, StandardCharsets.US_ASCII);
            int idx = pair.indexOf(':');
            if (idx < 0) {
                return new ClientCredentials(pair, "");
            }
            return new ClientCredentials(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return new ClientCredentials(request.getParameter("client_id"), request.getParameter("client_secret"));
    }

    public static class ClientCredentials {
        private final String clientId;
        private final String clientSecret;

        public ClientCredentials(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }
    }
}
