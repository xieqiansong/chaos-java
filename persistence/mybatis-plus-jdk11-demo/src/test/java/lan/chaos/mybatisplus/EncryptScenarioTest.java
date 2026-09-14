package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.encrypt.EncryptScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字段级 AES 透明加密：落库密文、读取还原明文。
 */
@SpringBootTest
@Transactional
class EncryptScenarioTest {

    @Autowired
    private EncryptScenario encryptScenario;

    @Test
    void transparentEncrypt() {
        Map<String, Object> r = encryptScenario.transparentEncrypt();

        assertTrue((Boolean) r.get("decryptEqual"));   // 业务读取还原为明文
        assertTrue((Boolean) r.get("dbIsCipher"));      // 数据库里是密文（非明文）
        assertTrue((Boolean) r.get("cipherDecryptOk")); // 密文可被解密
    }
}
