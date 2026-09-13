package lan.chaos.crypto.common.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ★ 主流加密算法清单 —— 本模块的「目录 / 导航」。
 *
 * <p>WHY 单独建一个类：学密码学最大的困惑往往不是「某个 API 怎么调」，而是
 * 「一共有哪些算法、各自属于哪一类、现在该用谁、谁已经淘汰」。
 * 本类把主流算法按能力分类列成表（规格 / 推荐状态 / 典型用途），
 * 由 {@link #main} 打印，作为整个 demo 的第一站。
 *
 * <p>推荐状态四种含义：
 * <ul>
 *   <li>{@code 推荐}：新系统默认选择；</li>
 *   <li>{@code 可用}：能用，但有前提（要配 HMAC、密钥要够长、要足够迭代……）；</li>
 *   <li>{@code 遗留}：仅为兼容老系统保留，新系统不要用；</li>
 *   <li>{@code 禁用}：已被攻破或存在结构性缺陷，安全场景禁止使用。</li>
 * </ul>
 *
 * <p>记住一条总原则：<b>不要自己发明密码协议</b>，优先用现成的 AEAD（AES-GCM / ChaCha20-Poly1305）
 * 与成熟的密钥管理（KMS / HSM）。
 */
public final class CryptoAlgorithmCatalog {

    private CryptoAlgorithmCatalog() {
    }

    // ------------------------------------------------------------------ 分类

    /** 能力大类。 */
    public enum Category {
        SYMMETRIC("对称加密"),
        ASYMMETRIC("非对称加密"),
        DIGEST("摘要 / 哈希"),
        MAC("消息认证码 MAC"),
        KDF("密钥派生 KDF"),
        ENCODING("编码（非加密）");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 推荐状态。 */
    public enum Status {
        RECOMMENDED("推荐"),
        USABLE("可用"),
        LEGACY("遗留"),
        BROKEN("禁用");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 一条算法条目。 */
    public static final class Algorithm {
        private final Category category;
        private final String name;
        private final String spec;
        private final Status status;
        private final String usage;

        Algorithm(Category category, String name, String spec, Status status, String usage) {
            this.category = category;
            this.name = name;
            this.spec = spec;
            this.status = status;
            this.usage = usage;
        }

        public Category category() {
            return category;
        }

        public String name() {
            return name;
        }

        public String spec() {
            return spec;
        }

        public Status status() {
            return status;
        }

        public String usage() {
            return usage;
        }
    }

    // ------------------------------------------------------------------ 清单

    private static final List<Algorithm> ALL;

    static {
        List<Algorithm> l = new ArrayList<Algorithm>();

        // ===== 对称加密：加解密同一把密钥，快，用于业务数据 =====
        l.add(a(Category.SYMMETRIC, "AES-128/192/256", "分组 128 位，密钥 128/192/256 位", Status.RECOMMENDED,
                "业务数据加密的事实标准，优先用 AES/GCM/NoPadding"));
        l.add(a(Category.SYMMETRIC, "AES-GCM", "AEAD：96 位 nonce + 128 位 tag", Status.RECOMMENDED,
                "机密性 + 完整性一次到位，新项目首选；nonce 绝不可复用"));
        l.add(a(Category.SYMMETRIC, "AES-CBC", "需随机 128 位 IV", Status.USABLE,
                "只保机密性，被篡改不报错，必须另配 HMAC（Encrypt-then-MAC）"));
        l.add(a(Category.SYMMETRIC, "AES-CTR", "流式，需唯一 nonce", Status.USABLE,
                "支持随机访问 / 流式处理，同样需要单独的 MAC"));
        l.add(a(Category.SYMMETRIC, "AES-CFB / OFB", "流式分组模式", Status.USABLE,
                "老协议兼容场景，新系统无必要"));
        l.add(a(Category.SYMMETRIC, "AES-ECB", "无 IV，逐块独立加密", Status.BROKEN,
                "相同明文块产生相同密文，泄露数据轮廓（经典 ECB 企鹅图）"));
        l.add(a(Category.SYMMETRIC, "ChaCha20-Poly1305", "256 位密钥 + 96 位 nonce", Status.RECOMMENDED,
                "AEAD；无 AES 硬件加速（部分移动端 / 嵌入式）时比 AES-GCM 更快"));
        l.add(a(Category.SYMMETRIC, "SM4", "分组 128 位，密钥 128 位", Status.RECOMMENDED,
                "国密对称算法，对标 AES；政务 / 金融 / 国企合规场景必选"));
        l.add(a(Category.SYMMETRIC, "DES", "56 位密钥", Status.BROKEN,
                "密钥太短，可被暴力破解，禁止使用"));
        l.add(a(Category.SYMMETRIC, "3DES / DESede", "112 / 168 位有效密钥", Status.LEGACY,
                "受 Sweet32 生日攻击影响，NIST 已弃用，仅用于读老数据"));
        l.add(a(Category.SYMMETRIC, "RC4", "流密码", Status.BROKEN,
                "密钥流存在统计偏差，已被实际攻破（WEP/TLS 历史教训）"));
        l.add(a(Category.SYMMETRIC, "Blowfish", "32~448 位密钥，分组仅 64 位", Status.LEGACY,
                "分组太小、密钥调度慢，已被 AES 取代"));
        l.add(a(Category.SYMMETRIC, "Camellia / ARIA / SEED", "128 位分组", Status.USABLE,
                "日 / 韩区域标准，生态与硬件支持有限，非合规场景不必要"));

        // ===== 非对称加密：公私钥成对，解决密钥分发与身份 =====
        l.add(a(Category.ASYMMETRIC, "RSA-2048/3072/4096", "大整数分解难题", Status.RECOMMENDED,
                "加密必须用 OAEP 填充，签名推荐 PSS；只加密小数据 / 密钥，别加密业务大块数据"));
        l.add(a(Category.ASYMMETRIC, "RSA-PKCS1v1.5 填充", "老式填充", Status.LEGACY,
                "存在 Bleichenbacher 类填充预言风险，新系统改用 OAEP / PSS"));
        l.add(a(Category.ASYMMETRIC, "ECDSA (P-256/P-384)", "椭圆曲线离散对数", Status.RECOMMENDED,
                "同安全强度下密钥与签名远短于 RSA，广泛用于证书 / 区块链"));
        l.add(a(Category.ASYMMETRIC, "Ed25519 (EdDSA)", "Curve25519，256 位", Status.RECOMMENDED,
                "现代签名首选：确定性签名、无随机数陷阱、速度快；JDK8 需 BouncyCastle"));
        l.add(a(Category.ASYMMETRIC, "X25519", "Curve25519 上的 DH", Status.RECOMMENDED,
                "现代密钥协商，TLS 1.3 默认套件之一；JDK8 需 BouncyCastle"));
        l.add(a(Category.ASYMMETRIC, "ECDH", "椭圆曲线 Diffie-Hellman", Status.RECOMMENDED,
                "前向保密（PFS）的基石，会话密钥协商"));
        l.add(a(Category.ASYMMETRIC, "DH (Diffie-Hellman)", "离散对数，建议 2048 位以上", Status.USABLE,
                "密钥协商的鼻祖，须用足够大的素数组（≥2048 位）"));
        l.add(a(Category.ASYMMETRIC, "DSA", "1024/2048/3072 位", Status.LEGACY,
                "仅签名不加密，已被 ECDSA / Ed25519 取代"));
        l.add(a(Category.ASYMMETRIC, "ElGamal", "离散对数", Status.LEGACY,
                "密文膨胀一倍，仅教学意义（PGP 历史实现）"));
        l.add(a(Category.ASYMMETRIC, "SM2", "256 位椭圆曲线", Status.RECOMMENDED,
                "国密非对称算法，对标 RSA / ECDSA，签名用 SM3withSM2"));

        // ===== 摘要 / 哈希：单向压缩成固定长度指纹 =====
        l.add(a(Category.DIGEST, "SHA-256/384/512", "SHA-2 家族", Status.RECOMMENDED,
                "完整性校验、HMAC、数字签名的通用基础"));
        l.add(a(Category.DIGEST, "SHA-3 (SHA3-256/512)", "Keccak 海绵结构，与 SHA-2 完全不同", Status.RECOMMENDED,
                "SHA-2 之外的结构性备选；JDK8 需 BouncyCastle"));
        l.add(a(Category.DIGEST, "SHA-1", "160 位", Status.BROKEN,
                "碰撞已被实际构造（SHAttered），禁止用于签名与证书"));
        l.add(a(Category.DIGEST, "MD5", "128 位", Status.BROKEN,
                "碰撞秒破，仅可做非安全校验和（如去重指纹）"));
        l.add(a(Category.DIGEST, "SM3", "256 位", Status.RECOMMENDED,
                "国密哈希，对标 SHA-256；国密签名 SM2 的配套哈希"));
        l.add(a(Category.DIGEST, "BLAKE2 / BLAKE3", "256~512 位", Status.USABLE,
                "高性能哈希（比 SHA-2 快），非 NIST 标准，适合性能敏感场景"));
        l.add(a(Category.DIGEST, "RIPEMD-160", "160 位", Status.LEGACY,
                "仅见于比特币地址等老场景，新系统不用"));
        l.add(a(Category.DIGEST, "CRC32 / Adler-32", "32 位校验和", Status.BROKEN,
                "只是传输校验和，不具备抗碰撞能力，绝不能当安全摘要"));

        // ===== MAC：带密钥的摘要，验证完整性与来源 =====
        l.add(a(Category.MAC, "HMAC-SHA256", "任意长密钥，32 字节输出", Status.RECOMMENDED,
                "接口签名（如「HMAC 鉴权」）、JWT HS256、消息完整性；比裸摘要多一把密钥"));
        l.add(a(Category.MAC, "HMAC-SHA1", "20 字节输出", Status.LEGACY,
                "老协议 / 老 SDK 里仍能见到，新系统用 HMAC-SHA256"));
        l.add(a(Category.MAC, "CMAC-AES (OMAC)", "分组密码构造的 MAC", Status.RECOMMENDED,
                "有 AES 硬件加速时比 HMAC 更快，适合嵌入式"));
        l.add(a(Category.MAC, "GMAC", "GCM 模式内建认证", Status.USABLE,
                "随 AES-GCM 一起出现，无需额外算法"));
        l.add(a(Category.MAC, "Poly1305", "一次性密钥，16 字节输出", Status.RECOMMENDED,
                "与 ChaCha20 配对组成 AEAD，禁止复用密钥"));
        l.add(a(Category.MAC, "CBC-MAC", "分组密码构造", Status.LEGACY,
                "存在长度扩展等坑，一律用 CMAC 代替"));

        // ===== 密钥派生：从口令 / 共享秘密派生密钥 =====
        l.add(a(Category.KDF, "PBKDF2", "盐 + 迭代次数（HMAC-SHA256）", Status.USABLE,
                "口令派生密钥；迭代次数必须够高（≥10 万，按硬件调优）"));
        l.add(a(Category.KDF, "bcrypt", "cost 因子，内存占用小", Status.RECOMMENDED,
                "密码存储老牌选择，自带盐；注意 72 字节输入上限"));
        l.add(a(Category.KDF, "scrypt", "N / r / p 参数，内存硬", Status.RECOMMENDED,
                "抗 GPU / ASIC 暴力破解，密码存储"));
        l.add(a(Category.KDF, "Argon2 (id / i / d)", "内存硬 + 可并行", Status.RECOMMENDED,
                "密码哈希竞赛冠军，当前密码存储首选"));
        l.add(a(Category.KDF, "HKDF", "提取-扩展两阶段", Status.RECOMMENDED,
                "从共享秘密 / 主密钥派生出多把用途隔离的子密钥"));
        l.add(a(Category.KDF, "直接 SHA-256(口令)", "无盐、无慢化", Status.BROKEN,
                "彩虹表与 GPU 暴力破解可秒破，密码存储绝对禁止"));

        // ===== 编码：不是加密，只解决「怎么表示 / 怎么传输」 =====
        l.add(a(Category.ENCODING, "Base64", "每 3 字节 → 4 字符", Status.USABLE,
                "二进制转文本（密文落库 / HTTP 传输）；无任何保密性"));
        l.add(a(Category.ENCODING, "Base64URL", "URL 安全变体（-_ 替代 +/）", Status.USABLE,
                "JWT、URL 参数等场景"));
        l.add(a(Category.ENCODING, "Hex", "每字节 → 2 字符", Status.USABLE,
                "调试 / 展示友好，但体积是原文 2 倍"));
        l.add(a(Category.ENCODING, "URL / Percent 编码", "转义特殊字符", Status.USABLE,
                "传输转义，与密码学无关"));

        ALL = Collections.unmodifiableList(l);
    }

    private static Algorithm a(Category c, String name, String spec, Status s, String usage) {
        return new Algorithm(c, name, spec, s, usage);
    }

    /** 全部算法条目（不可变）。 */
    public static List<Algorithm> all() {
        return ALL;
    }

    /** 按分类取条目，便于测试 / 筛选。 */
    public static List<Algorithm> byCategory(Category category) {
        List<Algorithm> out = new ArrayList<Algorithm>();
        for (Algorithm algo : ALL) {
            if (algo.category() == category) {
                out.add(algo);
            }
        }
        return out;
    }

    /** 统计各推荐状态的条目数，形如 {@code 推荐=26, 可用=14, 遗留=8, 禁用=7}。 */
    public static String statusSummary() {
        StringBuilder sb = new StringBuilder();
        Status[] statuses = Status.values();
        for (int i = 0; i < statuses.length; i++) {
            Status s = statuses[i];
            int count = 0;
            for (Algorithm algo : ALL) {
                if (algo.status() == s) {
                    count++;
                }
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(s.label()).append('=').append(count);
        }
        return sb.toString();
    }

    /** 控制台打印整张清单。 */
    public static void main(String[] args) {
        printCatalog();
    }

    public static void printCatalog() {
        System.out.println("==================== 主流加密算法清单 ====================");
        for (Category c : Category.values()) {
            System.out.println("\n【" + c.label() + "】");
            for (Algorithm algo : byCategory(c)) {
                System.out.printf("  [%s] %-22s %s%n", algo.status().label(), algo.name(), algo.spec());
                System.out.println("        用途：" + algo.usage());
            }
        }
        System.out.println("\n--------------------------------------------------------");
        System.out.println("合计 " + ALL.size() + " 项：" + statusSummary());
        System.out.println("选型口诀：对称用 AES-GCM / ChaCha20-Poly1305，非对称用 Ed25519 + X25519（或 RSA-OAEP/PSS），");
        System.out.println("         完整性用 HMAC-SHA256，密码存储用 Argon2 / bcrypt，合规场景换国密 SM2/SM3/SM4。");
    }
}
