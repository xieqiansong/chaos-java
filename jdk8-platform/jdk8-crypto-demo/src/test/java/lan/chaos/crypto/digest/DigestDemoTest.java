package lan.chaos.crypto.digest;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DigestDemoTest {

    private static final byte[] ABC = "abc".getBytes(StandardCharsets.UTF_8);

    @Test
    void knownAnswerTestsForEveryAlgorithm() throws Exception {
        // 标准测试向量（FIPS 180-4 / FIPS 202 / GB/T 32905），用来确认算法实现没被 Provider 掉包
        assertThat(DigestDemo.md5Hex(ABC)).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
        assertThat(DigestDemo.sha1Hex(ABC)).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
        assertThat(DigestDemo.sha256Hex(ABC)).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(DigestDemo.sha384Hex(ABC)).isEqualTo("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed"
                + "8086072ba1e7cc2358baeca134c825a7");
        assertThat(DigestDemo.sha512Hex(ABC)).isEqualTo("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f");
        assertThat(DigestDemo.sha3_256Hex(ABC)).isEqualTo("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532");
    }

    @Test
    void outputLengthsMatchTheAlgorithmSpec() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] data = sample.toBytes();

        assertThat(DigestDemo.md5Hex(data)).hasSize(32);
        assertThat(DigestDemo.sha1Hex(data)).hasSize(40);
        assertThat(DigestDemo.sha256Hex(data)).hasSize(64);
        assertThat(DigestDemo.sha384Hex(data)).hasSize(96);
        assertThat(DigestDemo.sha512Hex(data)).hasSize(128);
        assertThat(DigestDemo.sha3_256Hex(data)).hasSize(64);
    }

    @Test
    void deterministicAndAvalanche() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        String first = DigestDemo.sha256Hex(sample.toBytes());

        assertThat(DigestDemo.sha256Hex(sample.toBytes())).isEqualTo(first);
        assertThat(DigestDemo.sha256Hex((sample.plaintext() + "x").getBytes(StandardCharsets.UTF_8)))
                .isNotEqualTo(first);
    }

    @Test
    void chunkedDigestEqualsWholeDigest() throws Exception {
        byte[] data = new byte[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        assertThat(DigestDemo.sha256HexOfChunks(data, 7)).isEqualTo(DigestDemo.sha256Hex(data));
    }
}
