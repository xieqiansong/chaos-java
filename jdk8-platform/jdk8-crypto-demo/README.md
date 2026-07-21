# jdk8-crypto-demo  ★ A 类（加密与签名）

一句话定位：用一个「样例明文」横向覆盖四类密码学能力——**AES 对称加密（CBC/GCM）、RSA 非对称加密+签名、SHA-256 摘要、国密 SM2/SM3/SM4**，每个能力都有「输入→密文/摘要→还原」可观察输出，并标注生产坑。零外部服务依赖（国密靠 BouncyCastle 库）。

- 基础包：`lan.chaos.crypto`
- 技术栈：Spring Boot 2.7.18 + BouncyCastle bcprov-jdk18on 1.78（JDK8 兼容）
- 验证入口：`src/test/.../*Test`（每个能力均可断言往返一致；GCM/SM2 额外验证「篡改即失败」）
- 启动类：`CryptoApplication`（控制台分节打印四种方案的输出）

> 使用频率：`★★★ 高频`。

## 目录结构

```
jdk8-crypto-demo/
├── pom.xml
└── src/main/java/lan/chaos/crypto
    ├── CryptoApplication.java              # 启动类（分节打印）
    ├── common/
    │   ├── model/CryptoSample.java          # 样例明文（sampleSecret 工厂）
    │   └── util/HexUtil.java                # 字节十六进制化，便于观察密文/摘要
    ├── aes/AesDemo.java                      # AES/CBC + AES/GCM ★★★
    ├── rsa/RsaDemo.java                      # RSA 公钥加密 + SHA256withRSA 签名 ★★★
    ├── digest/DigestDemo.java               # SHA-256 摘要 ★★★
    └── sm/SmCryptoDemo.java                 # 国密 SM4/SM3/SM2（BouncyCastle）★★★
```

## 场景一览

`★★★ 高频`
- [AES 对称加密](#1-aes-对称加密) → CBC（机密性）/ GCM（机密性+完整性）
- [RSA 非对称加密 + 签名](#2-rsa-非对称加密--签名) → 公钥加密、私钥签名验签
- [SHA-256 摘要](#3-sha-256-摘要) → 完整性校验、密码存储误区
- [国密 SM2/SM3/SM4](#4-国密-sm2sm3sm4) → 合规场景的对称/哈希/签名

---

### 1. AES 对称加密 `★★★`

对称加密金标准：同一把密钥加解密，速度快，适合加密大量业务数据。
- `CBC`：需随机 IV，仅保机密性（被篡改不报错）→ 用 `AES/CBC/PKCS5Padding`。
- `GCM`：认证加密（AEAD），保机密性+完整性，自带 MAC → 推荐默认，用 `AES/GCM/NoPadding`。

生产坑：密钥不放硬编码（用 KMS）；IV 每次随机；CBC 无完整性需配 HMAC 或改 GCM；GCM 的 IV 复用会泄露；JDK8 默认限 128 位。
验证：`AesDemoTest` — CBC/GCM 往返还原；GCM 篡改密文抛 `AEADBadTagException`。

---

### 2. RSA 非对称加密 + 签名 `★★★`

解决「密钥分发」：公钥公开、私钥保密。加密（公钥加密/私钥解密）+ 签名（私钥签/公钥验）。
- 加密：`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`（必须用 OAEP，别用老 PKCS1）。
- 签名：`SHA256withRSA`。

生产坑：**RSA 不加密大块数据**（有长度上限）——正确做法混合加密（RSA 加密临时 AES 密钥，业务数据用 AES）；私钥放 HSM/密钥库。
验证：`RsaDemoTest` — 加密解密还原；签名验签通过、篡改原文验签失败。

---

### 3. SHA-256 摘要 `★★★`

把任意数据压成固定长度指纹（单向不可逆），用于完整性校验。
生产坑：**密码绝不能明文/只存 SHA-256**，必须加盐 + 慢哈希（bcrypt/scrypt/Argon2）；MD5/SHA-1 已不安全。
验证：`DigestDemoTest` — 同输入摘要一致、长度 64、不同输入不同。

---

### 4. 国密 SM2/SM3/SM4 `★★★`

中国商用密码标准，政务/金融/国企常要求，均由 BouncyCastle 提供者实现：
- `SM4`：对称（128 位密钥），对标 AES → `Cipher("SM4/CBC/PKCS5Padding","BC")`。
- `SM3`：哈希（256 位），对标 SHA-256 → `MessageDigest("SM3","BC")`。
- `SM2`：椭圆曲线非对称，对标 RSA → `KeyPairGenerator("SM2","BC")` + `Signature("SM3withSM2","BC")`。

生产坑：必须注册并指定 `"BC"` 提供者；SM2 用 `SM3withSM2`（先 SM3 再 SM2 签名）；SM4 同样随机 IV。
验证：`SmCryptoDemoTest` — SM4 往返、SM3 长度 64、SM2 签名验签（篡改失败）。

---

## 如何运行

```bash
# 1) 跑测试（纯内存，零外部依赖，任何环境直接过）
mvn -pl jdk8-crypto-demo test

# 2) 控制台看「输入→输出」：运行 CryptoApplication.main
#    （或 mvn -pl jdk8-crypto-demo spring-boot:run）
```

预期（节选）：
```
[AES/CBC] 密文=3f1a...  解密后=secret: 订单金额=100&用户=Alice
[AES/GCM] 密文(含tag)=a9c2...  解密后=secret: ...
[RSA] 解密后=secret: ...
[RSA] 签名验签=true
[SHA-256] 8b1f...
[SM4] 密文=...  解密后=secret: ...
[SM3] 7e3c...
[SM2] 签名验签=true
```

## 进阶方向（生产考量）

- `◆` **混合加密**：RSA 只加密临时 AES 密钥，业务数据用 AES-GCM（TLS 思路）。
- `◆` **密钥管理**：KMS / Vault 托管密钥，禁用硬编码；轮转策略。
- `◆` **密码存储**：bcrypt/scrypt/Argon2 加盐慢哈希，绝不用纯摘要。
- `◆` **合规**：金融/政务优先国密套件（SM2/SM3/SM4/SM9），证书用国密 CA。

## 设计要点

- **同一样例明文**：四种方案处理同一段 `CryptoSample`，对比公平、可断言「解密后==原文」。
- **能力即顶层包**：`aes/rsa/digest/sm` 各自聚焦一类能力，一个类讲清一个知识点 + WHY 注释。
- **可观察 + 可断言**：既打印输入→输出，也测「篡改即失败」（GCM/SM2），讲清认证加密价值。
- **频率结论**：AES-GCM（数据加密）+ RSA（密钥/签名）+ SHA-256（完整性）是生产最常用；国密是合规必选项。
