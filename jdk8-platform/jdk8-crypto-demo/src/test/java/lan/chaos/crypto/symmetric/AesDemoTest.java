package lan.chaos.crypto.symmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesDemoTest {

    @Test
    void allModesRoundTripPreservePlaintext() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = AesDemo.genKey(128);

        Map<String, byte[]> ivByTransformation = new LinkedHashMap<String, byte[]>();
        ivByTransformation.put(AesDemo.ECB, null);
        ivByTransformation.put(AesDemo.CBC, AesDemo.genIv());
        ivByTransformation.put(AesDemo.CTR, AesDemo.genIv());
        ivByTransformation.put(AesDemo.CFB, AesDemo.genIv());
        ivByTransformation.put(AesDemo.OFB, AesDemo.genIv());
        ivByTransformation.put(AesDemo.GCM, AesDemo.genNonce());

        for (Map.Entry<String, byte[]> entry : ivByTransformation.entrySet()) {
            String transformation = entry.getKey();
            byte[] iv = entry.getValue();

            byte[] ciphertext = AesDemo.encrypt(transformation, key, iv, sample.toBytes());
            byte[] plaintext = AesDemo.decrypt(transformation, key, iv, ciphertext);

            assertThat(new String(ciphertext)).as("%s 密文不应等于明文", transformation).isNotEqualTo(sample.plaintext());
            assertThat(plaintext).as("%s 解密后应还原", transformation).isEqualTo(sample.toBytes());
        }
    }

    @Test
    void aes256KeyIsSupported() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = AesDemo.genKey(256);
        assertThat(key).hasSize(32);

        byte[] nonce = AesDemo.genNonce();
        byte[] ciphertext = AesDemo.encryptGcm(key, nonce, sample.toBytes());
        assertThat(AesDemo.decryptGcm(key, nonce, ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void gcmFailsOnTamperedCiphertext() throws Exception {
        byte[] key = AesDemo.genKey(128);
        byte[] nonce = AesDemo.genNonce();
        byte[] ciphertext = AesDemo.encryptGcm(key, nonce, "hello".getBytes("UTF-8"));

        ciphertext[ciphertext.length - 1] ^= 0x01;
        assertThatThrownBy(() -> AesDemo.decryptGcm(key, nonce, ciphertext))
                .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    void gcmAadTakesPartInAuthentication() throws Exception {
        byte[] key = AesDemo.genKey(128);
        byte[] nonce = AesDemo.genNonce();
        byte[] aad = "tenantId=42".getBytes("UTF-8");
        byte[] ciphertext = AesDemo.encryptGcmWithAad(key, nonce, aad, "data".getBytes("UTF-8"));

        assertThat(AesDemo.decryptGcmWithAad(key, nonce, aad, ciphertext)).isEqualTo("data".getBytes("UTF-8"));
        // AAD 被改动后认证失败：把「不可篡改的元数据」绑进密文
        assertThatThrownBy(() -> AesDemo.decryptGcmWithAad(key, nonce, "tenantId=43".getBytes("UTF-8"), ciphertext))
                .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    void cbcCannotDetectTamperingWhileGcmAlwaysDoes() throws Exception {
        byte[] key = AesDemo.genKey(128);
        byte[] iv = AesDemo.genIv();
        byte[] plaintext = "0123456789abcdef0123456789abcdef0123456789abcdef".getBytes("UTF-8");

        // 篡改中间密文块：最后一块的填充不受影响，CBC 会「成功」解出一段错误明文，不报任何错
        byte[] cbc = AesDemo.encryptCbc(key, iv, plaintext);
        cbc[20] ^= 0x01;
        byte[] decrypted = AesDemo.decryptCbc(key, iv, cbc);
        assertThat(java.util.Arrays.equals(plaintext, decrypted))
                .as("CBC 没有完整性保护：篡改后静默返回错误明文")
                .isFalse();

        // 而 GCM 对任意一位篡改都会认证失败 —— 这就是认证加密（AEAD）的价值
        byte[] nonce = AesDemo.genNonce();
        byte[] gcm = AesDemo.encryptGcm(key, nonce, plaintext);
        gcm[20] ^= 0x01;
        assertThatThrownBy(() -> AesDemo.decryptGcm(key, nonce, gcm))
                .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    void ecbRevealsRepeatedBlocksWhileCbcDoesNot() throws Exception {
        byte[] key = AesDemo.genKey(128);
        byte[] repeated = new byte[32]; // 两个完全相同的 AES 分组

        byte[] ecb = AesDemo.encrypt(AesDemo.ECB, key, null, repeated);
        assertThat(slice(ecb, 0, 16)).as("ECB 相同明文块产生相同密文").isEqualTo(slice(ecb, 16, 32));

        byte[] cbc = AesDemo.encrypt(AesDemo.CBC, key, AesDemo.genIv(), repeated);
        assertThat(slice(cbc, 0, 16)).as("CBC 链式结构隐藏了重复").isNotEqualTo(slice(cbc, 16, 32));
    }

    private static byte[] slice(byte[] data, int from, int to) {
        byte[] out = new byte[to - from];
        System.arraycopy(data, from, out, 0, out.length);
        return out;
    }
}
