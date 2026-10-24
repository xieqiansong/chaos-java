package lan.chaos.oauth2.auth.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE（Proof Key for Code Exchange，RFC 7636）工具。
 *
 * <p>WHY：公共客户端（SPA/移动端）无法安全保存 client_secret，授权码在重定向过程中
 * 可能被拦截。PKCE 让客户端先生成一个随机 verifier，用 S256 算出 challenge 随授权请求发出；
 * 换 Token 时再出示 verifier，授权服务器比对 challenge，从而证明「换 Token 的客户端」
 * 就是「发起授权的客户端」，防授权码注入/拦截。</p>
 */
public final class PkceUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PkceUtil() {
    }

    /** 生成 43~128 字符的 URL-safe 随机 verifier（演示用 32 字节随机再 base64url）。 */
    public static String generateVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** S256：challenge = BASE64URL(SHA256(verifier))。 */
    public static String challengeS256(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** 校验 verifier 是否匹配 challenge。 */
    public static boolean verify(String verifier, String challenge) {
        if (verifier == null || challenge == null) {
            return false;
        }
        return challengeS256(verifier).equals(challenge);
    }
}
