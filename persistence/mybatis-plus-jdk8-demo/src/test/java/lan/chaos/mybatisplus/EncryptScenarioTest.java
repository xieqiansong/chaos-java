package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.common.context.TenantContext;
import lan.chaos.mybatisplus.encrypt.EncryptScenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EncryptScenarioTest {

    @Autowired
    private EncryptScenario encryptScenario;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        DynamicTableContext.clear();
    }

    @Test
    void transparentEncrypt() {
        Map<String, Object> r = encryptScenario.transparentEncrypt();
        assertThat((Boolean) r.get("decryptEqual")).isTrue();  // 读回明文一致
        assertThat((Boolean) r.get("dbIsCipher")).isTrue();    // 库中确实存密文
        assertThat((Boolean) r.get("cipherDecryptOk")).isTrue();
    }
}
