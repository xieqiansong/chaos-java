package lan.chaos.tools;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenValidator {

    // 服务端密钥 - 实际项目中应从安全存储（如配置中心、KMS）获取
    private static final byte[] SECRET_KEY = "YourSecureServerKey123!".getBytes();
    private static final int NONCE_LENGTH = 16; // 随机数长度（16字节=128位）
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // 生成Token
    public static String generateToken(String clientId) {
        try {
            // 1. 生成安全随机数
            byte[] nonce = generateNonce();

            // 2. 准备待签名的消息：客户端ID + 随机数
            byte[] message = concatenateArrays(clientId.getBytes(), nonce);

            // 3. 计算HMAC签名
            byte[] signature = calculateHmac(message);

            // 4. 组合随机数和签名
            byte[] tokenBytes = concatenateArrays(nonce, signature);

            // 5. 进行URL安全的Base64编码
            return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }

    // 验证Token
    public static boolean validateToken(String clientId, String token) {
        try {
            // 1. Base64解码
            byte[] tokenBytes = Base64.getUrlDecoder().decode(token);

            // 2. 分离随机数和签名
            byte[] nonce = new byte[NONCE_LENGTH];
            byte[] receivedSignature = new byte[tokenBytes.length - NONCE_LENGTH];
            System.arraycopy(tokenBytes, 0, nonce, 0, NONCE_LENGTH);
            System.arraycopy(tokenBytes, NONCE_LENGTH, receivedSignature, 0, receivedSignature.length);

            // 3. 重构消息：客户端ID + 随机数
            byte[] message = concatenateArrays(clientId.getBytes(), nonce);

            // 4. 重新计算签名
            byte[] calculatedSignature = calculateHmac(message);

            // 5. 安全比较签名（防止时序攻击）
            return constantTimeCompare(receivedSignature, calculatedSignature);

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] generateNonce() {
        byte[] nonce = new byte[NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    private static byte[] calculateHmac(byte[] message)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, HMAC_ALGORITHM);
        hmac.init(keySpec);
        return hmac.doFinal(message);
    }

    private static boolean constantTimeCompare(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static byte[] concatenateArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    // 测试示例
    public static void main(String[] args) {
        String clientId = "TEST001";

        // 生成Token
        String token1 = generateToken(clientId);
        System.out.println("Generated Token 1: " + token1);

        String token2 = generateToken(clientId);
        System.out.println("Generated Token 2: " + token2);

        // 验证Token
        System.out.println("Validate Token 1: " + validateToken(clientId, token1)); // true
        System.out.println("Validate Token 2: " + validateToken(clientId, token2)); // true

        // 错误情况测试
        System.out.println("Invalid Client ID: " + validateToken("WRONGID", token1)); // false
        System.out.println("Modified Token: " + validateToken(clientId, token1 + "x")); // false
    }
}
