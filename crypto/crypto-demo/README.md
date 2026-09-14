# jdk8-crypto-demo  ★ A 类（加密与签名）

一句话定位：**把主流加密算法一次性列全**——对称（AES 全模式 / ChaCha20-Poly1305 / SM4 / 遗留 DES-3DES-RC4）、非对称（RSA / ECDSA / Ed25519 / ECDH / X25519 / DH / SM2）、摘要（MD5 / SHA-1 / SHA-2 / SHA3-256 / SM3）、MAC（HMAC / CMAC）、KDF（PBKDF2 / HKDF）、编码（Base64 / Hex），每类都有「输入 → 密文/摘要 → 还原」可观察输出，并标注推荐状态与生产坑。

- 基础包：`lan.chaos.crypto`
- 技术栈：Spring Boot 2.7.18 + BouncyCastle `bcprov-jdk18on` 1.78（JDK8 原生缺失的算法全由 BC 补：SM 系列、SHA-3、ChaCha20-Poly1305、Ed25519/X25519、HKDF）
- 触发方式：`src/test/.../*Test`（13 个测试类 / 54 条用例）+ 控制台 `CryptoApplication.main`（分节打印全部场景）
- 外部依赖：**零**（纯内存、纯算法，任何环境直接跑）

> 使用频率：`★★★ 高频`。

## 目录结构

```
jdk8-crypto-demo/
├── pom.xml
└── src/main/java/lan/chaos/crypto
    ├── CryptoApplication.java                    # 启动类：先打印算法清单，再分节跑全部场景
    ├── common/
    │   ├── catalog/CryptoAlgorithmCatalog.java   # ★ 主流算法清单（47 项，含推荐/可用/遗留/禁用）
    │   ├── model/CryptoSample.java               # 样例明文（sampleSecret 工厂）
    │   └── util/
    │       ├── CryptoProviders.java              # BouncyCastle 提供者统一注册
    │       ├── HexUtil.java                      # 字节 ↔ 十六进制
    │       └── Base64Util.java                   # 字节 ↔ Base64（含 URL 安全变体）
    ├── symmetric/                                # 对称加密
    │   ├── AesDemo.java                          # AES：ECB/CBC/CTR/CFB/OFB/GCM + AAD ★★★
    │   ├── ChaCha20Demo.java                     # ChaCha20-Poly1305（BC，无 AES 硬件加速时首选）★★
    │   └── LegacyCipherDemo.java                 # DES / 3DES / RC4（已淘汰，反面对照）★
    ├── asymmetric/                               # 非对称加密
    │   ├── RsaDemo.java                          # RSA：OAEP/PKCS1 加密 + PKCS1/PSS 签名 ★★★
    │   ├── EccDemo.java                          # ECDSA 签名 + ECDH 密钥协商 ★★★
    │   ├── Ed25519Demo.java                      # Ed25519 签名 + X25519 协商（BC）★★★
    │   └── DhDemo.java                           # 经典 DH 密钥协商（对照 ECDH）★★
    ├── digest/                                   # 摘要与 MAC
    │   ├── DigestDemo.java                       # MD5/SHA-1/SHA-2/SHA3-256 对比 + 标准测试向量 ★★★
    │   └── MacDemo.java                          # HMAC-SHA256 / HMAC-SHA1 / CMAC-AES ★★★
    ├── kdf/KdfDemo.java                          # PBKDF2（口令）+ HKDF（共享秘密）★★★
    ├── sm/SmCryptoDemo.java                      # 国密 SM4(CBC+GCM) / SM3 / SM2（签名+加密）★★★
    └── practice/HybridCryptoDemo.java            # 混合加密：RSA-OAEP 包裹 AES-GCM 会话密钥 ★★★
```

## 场景一览

`★★★ 高频`
- [主流算法清单](#0-主流算法清单) → 47 项算法一次列全，含推荐状态与选型口诀
- [AES 对称加密](#1-aes-对称加密全模式) → ECB/CBC/CTR/CFB/OFB/GCM，看清模式差异
- [RSA 非对称](#2-rsa-非对称加密与签名) → OAEP 加密、PSS 签名、长度上限
- [摘要与 MAC](#3-摘要与消息认证码) → MD5/SHA-1/SHA-2/SHA3-256/HMAC/CMAC
- [密钥派生 KDF](#4-密钥派生-kdf) → PBKDF2 加盐慢化、HKDF 子密钥隔离
- [国密 SM2/SM3/SM4](#5-国密套件-sm2sm3sm4) → 合规场景的对称/哈希/非对称
- [混合加密实战](#6-混合加密实战) → RSA + AES 互补，TLS/KMS 的通用做法
- [椭圆曲线](#7-椭圆曲线ecdsa--ecdh--dh--ed25519--x25519) → ECDSA/ECDH/Ed25519/X25519/DH

`★★ 进阶`
- [ChaCha20-Poly1305](#8-chacha20-poly1305) → 无 AES 硬件加速场景的 AEAD
- [遗留算法对照](#9-遗留算法反面对照) → DES/3DES/RC4 为什么被淘汰

---

### 0. 主流算法清单 `★★★`

`CryptoAlgorithmCatalog` 是整个 demo 的导航：把主流算法按 6 类列成表，每条给出
「规格 / 推荐状态（推荐·可用·遗留·禁用）/ 典型用途」，运行时由 `CryptoApplication` 首先打印。

```
【对称加密】
  [推荐] AES-128/192/256    分组 128 位，密钥 128/192/256 位
        用途：业务数据加密的事实标准，优先用 AES/GCM/NoPadding
  [禁用] AES-ECB            无 IV，逐块独立加密
        用途：相同明文块产生相同密文，泄露数据轮廓（经典 ECB 企鹅图）
  ...
合计 47 项：推荐=20, 可用=12, 遗留=8, 禁用=7

选型口诀：对称用 AES-GCM / ChaCha20-Poly1305，非对称用 Ed25519 + X25519（或 RSA-OAEP/PSS），
         完整性用 HMAC-SHA256，密码存储用 Argon2 / bcrypt，合规场景换国密 SM2/SM3/SM4。
```

验证：`CryptoAlgorithmCatalogTest` — 6 大类均非空、关键算法齐备、条目字段完整且无重名、
MD5/SHA-1/DES/RC4/AES-ECB 必须标 `禁用`、推荐状态计数与明细一致。

---

### 1. AES 对称加密（全模式）`★★★`

对称加密金标准：同一把密钥加解密，速度快，适合加密大量业务数据。**不同工作模式的安全性差别巨大**：

| 模式 | 转换串 | IV | 完整性 | 结论 |
|---|---|---|---|---|
| GCM | `AES/GCM/NoPadding` | 12B nonce | ✅ AEAD 自带 tag | **默认选它** |
| CBC | `AES/CBC/PKCS5Padding` | 16B 随机 IV | ❌ 需另配 HMAC | 兼容场景 |
| CTR / CFB / OFB | `AES/CTR/NoPadding` 等 | 16B 唯一值 | ❌ 需另配 MAC | 流式场景 |
| ECB | `AES/ECB/PKCS5Padding` | 无 | ❌ | **禁用**（块间无扩散） |

生产坑：密钥不放硬编码（用 KMS）；IV/nonce 每次随机；GCM 的 nonce 复用会同时泄露明文与认证密钥；
CBC 被篡改只是解出乱码、不会报错，必须 Encrypt-then-MAC；JDK8u161 起默认已支持 AES-256。

验证：`AesDemoTest` — 6 种模式往返还原；GCM/AAD 篡改抛 `AEADBadTagException`；
ECB 下两块相同明文产生相同密文、CBC 则不同；CBC 中间块被篡改静默返回错误明文。

---

### 2. RSA 非对称加密与签名 `★★★`

解决「密钥分发」：公钥公开、私钥保密。加密（公钥加密/私钥解密）+ 签名（私钥签/公钥验）。

- 加密：`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`（推荐）vs `RSA/ECB/PKCS1Padding`（遗留）
- 签名：`SHA256withRSA`（确定性、兼容好）vs `RSASSA-PSS`（概率签名、推荐）

生产坑：**RSA 有明文长度上限**——2048 位 + OAEP-SHA256 单次最多 **190 字节**，
绝不用 RSA 直接加密业务大块数据（错误做法会抛 `IllegalBlockSizeException`）；
正确做法是混合加密（见场景 6）；私钥放 HSM/密钥库。

验证：`RsaDemoTest` — OAEP/PKCS1 往返；超长明文抛异常；PKCS1 签名验签 + 篡改失败；
PSS 验签 + 概率性（两次签名结果不同，PKCS1 则相同）；换公私钥验签失败。

---

### 3. 摘要与消息认证码 `★★★`

摘要把任意数据压成固定长度指纹（单向不可逆），用于完整性校验；MAC 再混入一把密钥，用于验证**来源**。

| 算法 | 输出 | 状态 | 说明 |
|---|---|---|---|
| MD5 | 128 位 | 禁用 | 碰撞秒破，仅做非安全校验和 |
| SHA-1 | 160 位 | 禁用 | SHAttered 已实际构造碰撞 |
| SHA-256/384/512 | 256/384/512 位 | 推荐 | 当前安全基线 |
| SHA3-256 | 256 位 | 推荐 | Keccak 结构，JDK8 需 BC |
| HMAC-SHA256 | 32 字节 | 推荐 | 接口签名 / JWT HS256 |
| CMAC-AES | 16 字节 | 推荐 | 有 AES 加速时比 HMAC 更快 |

生产坑：**密码绝不能只存 SHA-256**（必须加盐 + 慢哈希，见场景 4）；
**摘要 ≠ MAC**（`SHA256(msg)` 谁都能算，无法证明来源）；
比较 MAC 必须用 `MessageDigest.isEqual`（恒定时间），别用 `Arrays.equals`。

验证：`DigestDemoTest` — MD5/SHA-1/SHA-256/384/512/SHA3-256 全部标准测试向量（"abc"）；
输出长度符合规范；雪崩效应；分块 `update` 结果与整体一致。
`MacDemoTest` — HMAC-SHA256 标准向量；换密钥/改消息结果不同；CMAC 稳定；恒定时间比较。

---

### 4. 密钥派生 KDF `★★★`

用户口令是低熵的，**绝不能直接当密钥，也不能「SHA-256 一下」就存库**。KDF 用「加盐 + 慢化」抬高攻击成本。

- **PBKDF2**（`PBKDF2WithHmacSHA256`，JDK 原生）：靠迭代次数放大成本，演示 1,000 次 vs 200,000 次的耗时差。
- **HKDF**（BC）：把已随机的共享秘密（如 ECDH 结果）按 `info` 派生成多把用途隔离的子密钥。

生产坑：盐必须随机、每用户独立（≥16 字节）并与结果一起存库；PBKDF2 迭代建议 ≥10 万（OWASP 对 SHA-256 建议 60 万）；
密码存储如今首选 Argon2id，其次 bcrypt/scrypt。

验证：`KdfDemoTest` — 同盐同口令可复现、输出长度可配；换盐/换迭代/改口令结果全变；HKDF 同输入可复现、不同 `info` 互相隔离。

---

### 5. 国密套件 SM2/SM3/SM4 `★★★`

中国商用密码标准，政务/金融/国企常强制要求，全部由 BouncyCastle 提供（JDK8 原生没有）：

- `SM4`：对称（128 位密钥），对标 AES → `Cipher("SM4/CBC/PKCS5Padding","BC")`、`Cipher("SM4/GCM/NoPadding","BC")`
- `SM3`：哈希（256 位），对标 SHA-256 → `MessageDigest("SM3","BC")`
- `SM2`：椭圆曲线非对称（sm2p256v1），对标 RSA → `Signature("SM3withSM2","BC")`、`Cipher("SM2","BC")`

生产坑：**必须注册并指定 `"BC"` 提供者**，漏了就 `NoSuchAlgorithmException`；
SM2 签名用 `SM3withSM2`（先 SM3 再 SM2）；SM4 同样需要每次不同的 IV/nonce，推荐 SM4-GCM；
跨厂商对接要确认 SM2 密文编排顺序（C1C3C2 / C1C2C3）。

验证：`SmCryptoDemoTest` — SM4-CBC 往返、SM4-GCM 往返 + 篡改抛 `AEADBadTagException`；
SM3("abc") 标准向量 + 64 字符；SM2 签名验签 + 篡改失败；SM2 公钥加密 / 私钥解密往返。

---

### 6. 混合加密实战 `★★★`

RSA 与 AES 的短板互补，是 TLS 握手 / KMS DataKey / JWE 的通用做法：

1. 每次加密随机生成一把临时 AES-256 会话密钥；
2. 业务数据用 **AES-GCM** 加密（快、认证、不限长度）；
3. 会话密钥用 **RSA-OAEP 公钥**包裹，得到 `wrappedKey`；
4. 传输三元组 `wrappedKey + nonce + ciphertext`，解密方先解会话密钥再解数据。

验证：`HybridCryptoDemoTest` — 三元组往返还原；**每条消息的会话密钥/nonce/密文都不同**（绝不复用）；
错误私钥打不开信封；512KB 数据（远超 RSA 190 字节上限）正常往返。

---

### 7. 椭圆曲线（ECDSA / ECDH / DH / Ed25519 / X25519）`★★★`

同安全强度下 ECC 的密钥与签名远小于 RSA（P-256 签名 ~70 字节 vs RSA-3072 的 384 字节）：

- **ECDSA**（`SHA256withECDSA`，secp256r1/secp384r1）：签名；依赖随机数 k，k 复用会泄露私钥。
- **ECDH**（`KeyAgreement("ECDH")`）：双方交换公钥后独立算出同一共享秘密，前向保密的基础。
- **Ed25519**（BC）：确定性签名（无随机数陷阱），64 字节固定长度，现代签名首选。
- **X25519**（BC）：Curve25519 上的协商，TLS 1.3 默认套件。
- **DH**：协商思想鼻祖，密钥大一个数量级，新系统优先 ECDH/X25519。

生产坑：裸 DH/ECDH **不认证对端身份**，必须配合证书/签名防中间人；协商结果是原始字节，需经 HKDF 派生。

验证：`EccDemoTest`（签名长度对比、ECDH 双方一致、P-384 可用）、
`Ed25519DemoTest`（确定性签名、验签、X25519 一致）、`DhDemoTest`（协商一致、第三方不同）。

---

### 8. ChaCha20-Poly1305 `★★`

流密码 ChaCha20 + MAC Poly1305 组成的 AEAD。AES-GCM 在没有 AES-NI 的设备上会退化为软件实现且易受时序攻击，
ChaCha20 纯 ARX 设计、软件实现也快，是移动端 / 嵌入式的首选。

生产坑：Poly1305 密钥是一次性的，**同一密钥 + 同一 nonce 绝不能加密两段数据**；JDK8 必须指定 `"BC"`。

验证：`ChaCha20DemoTest` — 往返；密文 = 明文 + 16 字节 tag；篡改 / 错 nonce 均抛 `AEADBadTagException`。

---

### 9. 遗留算法反面对照 `★★`

真实世界的老系统里到处是它们，读懂才能做「兼容读取 + 判定禁用」：

- **DES**：56 位有效密钥，可暴力破解 → 禁用
- **3DES**：有效强度仅 112 位、分组 64 位，受 Sweet32 生日攻击 → 仅读老数据
- **RC4**：密钥流存在统计偏差，RFC 7465 已禁用 → 禁用

验证：`LegacyCipherDemoTest` — DES/3DES 往返；RC4 加解密同一运算（流密码特性）。

---

## 如何运行

```bash
# 1) 跑全部单测（纯内存，零外部依赖，54 条用例）
mvn -pl jdk8-crypto-demo test

# 2) 控制台看「输入→输出」：先打印算法清单，再分节跑 12 个场景
mvn -pl jdk8-crypto-demo spring-boot:run
#    或在 IDE 直接运行 lan.chaos.crypto.CryptoApplication.main
```

预期（节选）：

```
==================== 主流加密算法清单 ====================
【对称加密】
  [推荐] AES-128/192/256    分组 128 位，密钥 128/192/256 位
        用途：业务数据加密的事实标准，优先用 AES/GCM/NoPadding
  [禁用] AES-ECB            无 IV，逐块独立加密
...

==================== 对称加密 · AES 全模式（ECB/CBC/CTR/CFB/OFB/GCM） ====================
[AES] 密钥 128 位=3f1a...
  AES/CBC/PKCS5Padding     密文  48 字节  Base64=iVBORw0K...
  还原成功=true
  AES/GCM/NoPadding        密文  53 字节  Base64=oYvT...
  还原成功=true
[ECB 反例] 32 字节重复明文：
  ECB 密文=... → 前后两块完全相同，泄露「数据重复」这一信息
  CBC 密文=... → 前一块影响后一块，重复被隐藏
[GCM 篡改] 解密果断失败：AEADBadTagException → 这正是认证加密的价值（CBC 此时只会解出乱码）
...
[RSA 长度上限] 2048 位 + OAEP-SHA256 单次最多 190 字节；超长数据请改用混合加密
[Ed25519] 公钥 32 字节 / 签名 64 字节  验签=true
[HMAC-SHA256] 摘要=...  换个密钥=...  ← 密钥不同结果完全不同
[SM2] 签名验签=true，篡改后验签=false
[混合加密] Envelope{wrappedKey=256B, nonce=12B, ciphertext=53B}
  原文=1048576 字节  密文=1048592 字节  还原一致=true
```

## 进阶方向（生产考量）

- `◆` **密钥管理**：KMS / Vault / HSM 托管密钥，禁用硬编码，配轮转与版本化（`kms:keyId` 随密文一起存）。
- `◆` **密码存储**：Argon2id（首选）/ bcrypt / scrypt 加盐慢哈希 + 登录限流，绝不落明文或纯摘要。
- `◆` **AEAD 优先**：新协议一律用 AES-GCM / ChaCha20-Poly1305；确实要 CBC 就必须 Encrypt-then-MAC 并用恒定时间比较。
- `◆` **国密合规**：金融/政务优先国密套件（SM2/SM3/SM4/SM9）与国密 CA 证书；注意跨厂商 SM2 密文格式差异。
- `◆` **传输层**：别自己拼协议——优先 TLS 1.3（自带 ECDHE 前向保密 + AEAD）。
- `◆` **第三方库**：JCA 之外的场景可评估 Google Tink / libsodium，把「用错的概率」降到最低。

## 设计要点

- **清单先行**：`CryptoAlgorithmCatalog` 把 47 项算法按 6 类罗列并标注推荐状态，先回答「有哪些、该用谁」，再进入各场景 API 细节。
- **能力即顶层包**：`symmetric` / `asymmetric` / `digest` / `kdf` / `sm` / `practice`，一类能力一组类，一个类讲清一个知识点 + WHY 注释。
- **同一样例明文**：所有方案处理同一段 `CryptoSample`，对比公平、可断言「解密后 == 原文」。
- **可观察 + 可断言**：既打印输入→输出与长度差异，也测「篡改即失败」（GCM / ChaCha20 / SM4-GCM）与「反面对照」（ECB 重复块、CBC 静默错误、遗留算法淘汰原因）。
- **标准测试向量**：摘要/MAC/SM3 用官方向量（"abc"）做已知答案校验，避免 Provider 实现被调包而测试仍然「绿」。
- **频率结论**：AES-GCM（数据加密）+ Ed25519/X25519 或 RSA-OAEP/PSS（签名与协商）+ HMAC-SHA256（完整性）+ Argon2/bcrypt（口令）是生产主线；国密是合规必选项。
