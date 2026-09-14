package lan.chaos.crypto.common.catalog;

import lan.chaos.crypto.common.catalog.CryptoAlgorithmCatalog.Algorithm;
import lan.chaos.crypto.common.catalog.CryptoAlgorithmCatalog.Category;
import lan.chaos.crypto.common.catalog.CryptoAlgorithmCatalog.Status;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoAlgorithmCatalogTest {

    @Test
    void everyCategoryHasEntries() {
        for (Category category : Category.values()) {
            assertThat(CryptoAlgorithmCatalog.byCategory(category))
                    .as("分类 %s 不应为空", category.label())
                    .isNotEmpty();
        }
    }

    @Test
    void catalogCoversTheMainstreamAlgorithms() {
        Set<String> names = new HashSet<String>();
        for (Algorithm algorithm : CryptoAlgorithmCatalog.all()) {
            names.add(algorithm.name());
        }

        assertThat(names).contains(
                "AES-128/192/256", "AES-GCM", "ChaCha20-Poly1305", "SM4",
                "RSA-2048/3072/4096", "ECDSA (P-256/P-384)", "Ed25519 (EdDSA)", "X25519", "ECDH", "DH (Diffie-Hellman)", "SM2",
                "SHA-256/384/512", "SHA-3 (SHA3-256/512)", "SM3",
                "HMAC-SHA256", "CMAC-AES (OMAC)", "Poly1305",
                "PBKDF2", "Argon2 (id / i / d)", "HKDF",
                "Base64");
    }

    @Test
    void everyEntryIsCompleteAndUnique() {
        Set<String> names = new HashSet<String>();
        for (Algorithm algorithm : CryptoAlgorithmCatalog.all()) {
            assertThat(algorithm.name()).isNotBlank();
            assertThat(algorithm.spec()).isNotBlank();
            assertThat(algorithm.usage()).isNotBlank();
            assertThat(algorithm.status()).isNotNull();
            assertThat(names.add(algorithm.name())).as("算法名 %s 不应重复", algorithm.name()).isTrue();
        }
    }

    @Test
    void brokenAlgorithmsAreMarkedAsBroken() {
        assertThat(statusOf("MD5")).isEqualTo(Status.BROKEN);
        assertThat(statusOf("SHA-1")).isEqualTo(Status.BROKEN);
        assertThat(statusOf("DES")).isEqualTo(Status.BROKEN);
        assertThat(statusOf("RC4")).isEqualTo(Status.BROKEN);
        assertThat(statusOf("AES-ECB")).isEqualTo(Status.BROKEN);
        assertThat(statusOf("直接 SHA-256(口令)")).isEqualTo(Status.BROKEN);
    }

    @Test
    void recommendedAlgorithmsAreMarkedAsRecommended() {
        assertThat(statusOf("AES-GCM")).isEqualTo(Status.RECOMMENDED);
        assertThat(statusOf("Ed25519 (EdDSA)")).isEqualTo(Status.RECOMMENDED);
        assertThat(statusOf("Argon2 (id / i / d)")).isEqualTo(Status.RECOMMENDED);
        assertThat(statusOf("SM3")).isEqualTo(Status.RECOMMENDED);
    }

    @Test
    void statusSummaryCountsEveryEntry() {
        List<Algorithm> all = CryptoAlgorithmCatalog.all();
        String summary = CryptoAlgorithmCatalog.statusSummary();

        assertThat(all.size()).isGreaterThanOrEqualTo(40);
        for (Status status : Status.values()) {
            int actual = 0;
            for (Algorithm algorithm : all) {
                if (algorithm.status() == status) {
                    actual++;
                }
            }
            assertThat(summary).contains(status.label() + "=" + actual);
        }
    }

    private static Status statusOf(String name) {
        for (Algorithm algorithm : CryptoAlgorithmCatalog.all()) {
            if (algorithm.name().equals(name)) {
                return algorithm.status();
            }
        }
        throw new AssertionError("清单里没有算法：" + name);
    }
}
