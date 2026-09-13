package lan.chaos.crypto.symmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * ★ 反面对照：已被淘汰的对称算法 —— DES / 3DES(DESede) / RC4。
 *
 * <p>WHY 还要演示淘汰算法：真实世界里老系统、老协议、老固件里到处是它们，
 * 读懂它们才能做「兼容读取 + 判定禁用」；同时用它们的缺陷反衬现代算法的设计取舍。
 *
 * <ul>
 *   <li><b>DES</b>：56 位有效密钥，1998 年就被专用机器暴力破解，现在 GPU 集群小时级可破。<b>禁用</b>。</li>
 *   <li><b>3DES</b>：把 DES 跑三遍，有效强度仅 112 位，且分组仍是 64 位，
 *       海量数据下受 <i>Sweet32</i> 生日攻击（约 2^32 个分组即碰撞）。<b>仅用于读老数据</b>。</li>
 *   <li><b>RC4</b>：流密码，密钥流前若干字节存在统计偏差，WEP 被破、TLS 中被禁用（RFC 7465）。<b>禁用</b>。</li>
 * </ul>
 *
 * <p>唯一正确的演进路线：<b>DES → 3DES（兼容过渡）→ AES-GCM</b>。
 */
public class LegacyCipherDemo {

    private static final SecureRandom RNG = new SecureRandom();

    private LegacyCipherDemo() {
    }

    // ------------------------------------------------------------------ DES / 3DES

    /** 生成 DES 密钥（8 字节，其中 56 位有效）。 */
    public static byte[] genDesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("DES");
        return kg.generateKey().getEncoded();
    }

    /** 生成 3DES 密钥（24 字节）。 */
    public static byte[] genTripleDesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("DESede");
        return kg.generateKey().getEncoded();
    }

    public static byte[] desEncrypt(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        return ring("DES", "DES/CBC/PKCS5Padding", Cipher.ENCRYPT_MODE, key, iv, plaintext);
    }

    public static byte[] desDecrypt(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        return ring("DES", "DES/CBC/PKCS5Padding", Cipher.DECRYPT_MODE, key, iv, ciphertext);
    }

    public static byte[] tripleDesEncrypt(byte[] key, byte[] iv, byte[] plaintext) throws Exception {
        return ring("DESede", "DESede/CBC/PKCS5Padding", Cipher.ENCRYPT_MODE, key, iv, plaintext);
    }

    public static byte[] tripleDesDecrypt(byte[] key, byte[] iv, byte[] ciphertext) throws Exception {
        return ring("DESede", "DESede/CBC/PKCS5Padding", Cipher.DECRYPT_MODE, key, iv, ciphertext);
    }

    private static byte[] ring(String algorithm, String transformation, int mode,
                               byte[] key, byte[] iv, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(mode, new SecretKeySpec(key, algorithm), new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    // ------------------------------------------------------------------ RC4

    /** 生成 RC4 密钥（演示用 16 字节；RC4 允许 40~2048 位）。 */
    public static byte[] genRc4Key() {
        byte[] key = new byte[16];
        RNG.nextBytes(key);
        return key;
    }

    /** RC4 是流密码，加解密是同一段异或运算，因此只有一个方法。 */
    public static byte[] rc4(byte[] key, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RC4");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "RC4"));
        return cipher.doFinal(data);
    }

    public static void main(String[] args) throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();

        byte[] desIv = new byte[8];
        RNG.nextBytes(desIv);
        byte[] desKey = genDesKey();
        byte[] desCipher = desEncrypt(desKey, desIv, sample.toBytes());
        byte[] desPlain = desDecrypt(desKey, desIv, desCipher);
        System.out.println("[DES]    密钥=" + desKey.length * 8 + " 位（有效 56）密文=" + HexUtil.toHex(desCipher));
        System.out.println("         还原后=" + new String(desPlain) + "  ← 仅作反面对照，禁止用于生产");

        byte[] d3Iv = new byte[8];
        RNG.nextBytes(d3Iv);
        byte[] d3Key = genTripleDesKey();
        byte[] d3Cipher = tripleDesEncrypt(d3Key, d3Iv, sample.toBytes());
        byte[] d3Plain = tripleDesDecrypt(d3Key, d3Iv, d3Cipher);
        System.out.println("[3DES]   密钥=" + d3Key.length * 8 + " 位（有效 112），分组仅 64 位");
        System.out.println("         还原后=" + new String(d3Plain) + "  ← 受 Sweet32 影响，仅用于读老数据");

        byte[] rc4Key = genRc4Key();
        byte[] rc4Cipher = rc4(rc4Key, sample.toBytes());
        byte[] rc4Plain = rc4(rc4Key, rc4Cipher);
        System.out.println("[RC4]    密文=" + HexUtil.toHex(rc4Cipher));
        System.out.println("         还原后=" + new String(rc4Plain) + "  ← 密钥流有偏，RFC 7465 已禁用");
    }
}
