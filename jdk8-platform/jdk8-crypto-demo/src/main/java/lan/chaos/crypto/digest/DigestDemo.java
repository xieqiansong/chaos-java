package lan.chaos.crypto.digest;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.HexUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * ★★★ 高频：摘要（哈希）—— 把任意长度数据压缩成固定长度指纹，单向不可逆，用于完整性校验。
 *
 * <p>痛点：比对大文件/密码是否一致、防篡改，不能明文存储密码。SHA-256 是当前安全基线。
 *
 * <p>关键 API：{@code MessageDigest.getInstance("SHA-256")}。
 *
 * <p>生产坑：
 * <ul>
 *   <li><b>密码绝不能明文存储，也不能只存 SHA-256</b>（易被彩虹表/暴力破解）；
 *       必须加盐 + 慢哈希（bcrypt/scrypt/Argon2）。本 demo 仅演示摘要本身。</li>
 *   <li>MD5/SHA-1 已不安全（碰撞），新系统只用 SHA-256 及以上。</li>
 *   <li>不同编码/换行会导致摘要不同，比对前统一字符集。</li>
 * </ul>
 */
public class DigestDemo {

    /** 计算 SHA-256 十六进制摘要。 */
    public static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexUtil.toHex(md.digest(data));
    }

    public static void main(String[] args) throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        String h = sha256Hex(s.toBytes());
        System.out.printf("[SHA-256] %s%n  原文=%s%n", h, s.plaintext());
    }
}
