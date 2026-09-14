package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.util.HexUtil;

import javax.crypto.KeyAgreement;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * ★★ 可用：DH（Diffie-Hellman）密钥协商 —— 「密钥交换」概念的鼻祖。
 *
 * <p>解决了什么问题：Alice 和 Bob 要在<b>完全公开的信道</b>上商出一把只有他俩知道的密钥。
 * 原理是单向函数——双方各出一个私密随机数，公开算出对应公钥交换，
 * 再各自用自己的私密数 + 对方公钥算出同一个共享秘密；窃听者只有两个公开值，算不出结果
 * （这是「离散对数难题」）。
 *
 * <p>关键 API：{@code KeyPairGenerator("DH")} + {@code KeyAgreement("DH")}。
 * 与 {@link EccDemo} 的 ECDH 是同一思想的两套数学：DH 用大整数模幂，ECDH 用椭圆曲线点乘，
 * 后者密钥短得多、算得也快，因此新系统优先 ECDH / X25519。
 *
 * <p>生产坑：
 * <ul>
 *   <li><b>素数组要够大</b>：1024 位已被认为不安全，至少要 2048 位；且必须用标准素数组，
 *       自定义「弱素数」会让攻击者用数域筛法破解。</li>
 *   <li>裸 DH <b>不认证对端身份</b>，会被中间人攻击（MITM）——
 *       必须配合证书 / 签名做身份认证（这正是 TLS 做的事）。</li>
 *   <li>协商结果是原始共享秘密，需经 HKDF 派生后才可作为会话密钥。</li>
 * </ul>
 */
public class DhDemo {

    private DhDemo() {
    }

    /** 生成 DH 密钥对（bits 至少 2048）。 */
    public static KeyPair genKeyPair(int bits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }

    /** 用自己的私钥 + 对方公钥算出共享秘密；双方互换参数会得到相同结果。 */
    public static byte[] agreeSecret(PrivateKey myPrivate, PublicKey peerPublic) throws Exception {
        KeyAgreement agreement = KeyAgreement.getInstance("DH");
        agreement.init(myPrivate);
        agreement.doPhase(peerPublic, true);
        return agreement.generateSecret();
    }

    public static void main(String[] args) throws Exception {
        KeyPair alice = genKeyPair(2048);
        KeyPair bob = genKeyPair(2048);

        byte[] aliceSecret = agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] bobSecret = agreeSecret(bob.getPrivate(), alice.getPublic());

        System.out.println("[DH-2048] 公钥长度=" + alice.getPublic().getEncoded().length + " 字节（比 ECDH 大一个数量级）");
        System.out.println("  Alice 共享秘密=" + HexUtil.toHex(aliceSecret).substring(0, 32) + "...");
        System.out.println("  Bob   共享秘密=" + HexUtil.toHex(bobSecret).substring(0, 32) + "...");
        System.out.println("  双方一致=" + HexUtil.toHex(aliceSecret).equals(HexUtil.toHex(bobSecret)));
        System.out.println("  注意：裸 DH 不认证对端身份，必须配合证书/签名才能防中间人");
    }
}
