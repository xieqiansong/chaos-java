package lan.chaos.crypto.digest;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DigestDemoTest {

    @Test
    void sha256DeterministicAndFixedLength() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        String h1 = DigestDemo.sha256Hex(s.toBytes());
        String h2 = DigestDemo.sha256Hex(s.toBytes());

        assertEquals(h1, h2, "相同输入摘要应一致");
        assertEquals(64, h1.length(), "SHA-256 十六进制应为 64 字符");

        String h3 = DigestDemo.sha256Hex((s.plaintext() + "x").getBytes());
        assertNotEquals(h1, h3, "不同输入摘要应不同（雪崩）");
    }
}
