package lan.chaos.mybatisplus.encrypt;

import lan.chaos.mybatisplus.common.util.AesUtil;
import lan.chaos.mybatisplus.entity.Member;
import lan.chaos.mybatisplus.mapper.MemberMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 场景六：字段级 AES 透明加密。
 * phone 通过 AesTypeHandler 在落库前加密、读取时还原明文，业务代码完全无感。
 * 演示：明文写入 -> 落库密文 -> 查询还原明文 -> 用工具类解密密文验证一致。
 */
@Service
public class EncryptScenario {

    @Resource
    private MemberMapper memberMapper;

    /**
     * 透明加解密往返。返回三个断言点：
     *  - decryptEqual：业务查询读回的明文与原始一致（TypeHandler 自动还原）
     *  - dbIsCipher：数据库里实际存的是密文（不等于明文）
     *  - cipherDecryptOk：密文可被 AesUtil 解密回原文
     */
    public Map<String, Object> transparentEncrypt() {
        String name = "EncryptUser";
        String phone = "13800001234";

        Member m = new Member();
        m.setName(name);
        m.setPhone(phone);
        memberMapper.insert(m);
        Long id = m.getId();

        // 业务查询：AesTypeHandler 自动把密文还原为明文
        Member plain = memberMapper.selectById(id);

        // 绕过 TypeHandler 直读数据库原始值（密文），验证「落库即密文」
        String cipher = memberMapper.selectRawPhoneById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("decryptEqual", phone.equals(plain.getPhone()));
        result.put("dbIsCipher", !phone.equals(cipher));
        result.put("cipherDecryptOk", phone.equals(AesUtil.decrypt(cipher)));
        return result;
    }
}
