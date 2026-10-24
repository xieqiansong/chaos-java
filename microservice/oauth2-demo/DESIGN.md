# OAuth2.0 授权服务 + Token 生命周期管理 —— 开发文档

> 面向 `chaos-java` 仓库读者：本文件是**设计 + 实现方案**，先对齐思路再写代码。
> 配套代码落地于 **`microservice/oauth2-demo/`**（与 `security-demo` 同域，多模块，形态二）。
> 已确认：纯 API/控制台演示（无 HTML 页面）；Spring Boot 2.7.18 / JDK 8。

---

## 0. 与现有 `security-demo` 的分工（避免重复）

| 维度 | `microservice/security-demo` | 本 `oauth2-demo`（本文档） |
|---|---|---|
| 角色 | **资源服务器**消费方 | **授权服务器**（IdP）本身 |
| 关注点 | 过滤器链、JWT 校验、接外部 IdP | 客户端注册、授权码流程、签发/刷新/撤销/自省 Token |
| Token 来源 | Keycloak 等外部 IdP 签发 | 本服务自制签发（jjwt，RS256） |
| 结论 | 已覆盖「如何验」 | 补齐「如何发 + 如何管生命周期」 |

二者可串联：本 `auth-server` 扮演 `security-demo` 里那个「外部 IdP」的角色，通过 `JWK Set URI` 把公钥喂给资源服务器。本 demo 同时自带一个**最小资源服务器**和**第三方客户端**把整条链路跑通。

---

## 1. 定位与范围

**一句话定位**：手写一个最小可用的 OAuth2.0 授权服务器，覆盖「第三方应用接入（客户端注册/审核/凭证轮换）+ 授权码模式（含 PKCE）+ Token 全生命周期（生成→使用→刷新→过期→撤销）+ 自省端点」，并配资源服务器与第三方客户端跑通端到端流程。

**硬性约束（来自 `AGENTS.md`）**
- 不引 `spring-authorization-server` 这类「把机制藏进库」的组件——本仓库要讲清机制，故**手写**授权码/Token/刷新/撤销逻辑（与 `security-demo` 用 jjwt 手签 JWT 一脉相承）。
- 形态二多模块：按「部署/角色单元」拆模块，每进程一个 `<Role>Application`，模块内按能力分包，公共支撑收进各模块 `common/`。
- 根包 `lan.chaos.oauth2.*`；无 `demo` 层。
- 外部依赖最小化：Token/Code 存储默认**内存**（抽象 `TokenStore`，预留 Redis 实现），零中间件即可跑测试。
- 每个场景一个类 + `sampleXxx()` 样例数据 + WHY 注释 + 可观察「输入→输出」+ 至少一条可断言 `*Test`。

**范围（本期实现）**
- ✅ 授权码模式（Authorization Code）+ PKCE（公共客户端）。
- ✅ 客户端注册/审核/凭证轮换（内存实现，提供注册 API）。
- ✅ access_token = JWT（RS256，含 `jti/exp/scope/client_id/sub`）；refresh_token = 不透明随机串（存 `TokenStore`）。
- ✅ 授权码：`code` 一次性、短 TTL（10min）。
- ✅ 刷新：滚动刷新（发新 refresh 使旧失效，family 机制）。
- ✅ 撤销：单点（access/refresh 入黑名单）+ 按 client/user 批量撤销 + `/revoke` 端点。
- ✅ 自省：`/oauth2/introspect`（RFC 7662）+ `/.well-known/jwks.json` 供资源服务器验签。
- ✅ 安全要点落代码：HTTPS 注释指引、redirect_uri 强校验、state 防 CSRF、scope 最小化、client_secret 校验、日志审计点。
- ⏸ 非目标（进阶方向列出，不实现）：多租户、动态 scope 审批 UI、JWT 完全无状态撤销的分布式黑名单、设备码模式、OpenID Connect 完整 claims、真实 DB/Redis 持久化。

---

## 2. 架构与模块划分（形态二 / 多模块）

```
microservice/oauth2-demo/                      # 聚合模块（pom packaging=pom）
├── pom.xml                                     # 聚合 oauth2-auth-server / oauth2-resource-server / oauth2-client
├── oauth2-auth-server/                         # ★ 核心：授权服务器（IdP）
│   └── src/main/java/lan/chaos/oauth2/auth
│       ├── AuthServerApplication.java          # 启动类
│       ├── client/                             # 能力①：客户端注册与管理
│       │   ├── Client.java                     # 注册信息模型
│       │   ├── ClientRepository.java           # 内存仓库（seed 样例第三方应用）
│       │   ├── ClientRegistrationService.java   # 注册/审核/轮换 secret
│       │   └── ClientRegistrationEndpoint.java  # POST /oauth2/clients（演示用）
│       ├── authorize/                          # 能力②：授权码流程（纯 API）
│       │   ├── AuthorizeEndpoint.java          # GET /oauth2/authorize（Basic 登录→校验→发 consent/code）
│       │   ├── ConsentEndpoint.java            # POST /oauth2/consent（批准/拒绝→发 code）
│       │   ├── ConsentService.java             # scope→可读文案（UI 该渲染的数据）
│       │   ├── AuthorizationCodeService.java   # code 生成/单次消费/短 TTL
│       │   └── CodeRecord.java
│       ├── token/                              # 能力③：Token 签发与生命周期
│       │   ├── TokenEndpoint.java              # POST /oauth2/token（auth_code / refresh_token）
│       │   ├── AccessTokenIssuer.java          # JWT 签发（RS256，jti/exp/scope）
│       │   ├── RefreshTokenService.java        # 不透明 refresh + 滚动刷新 + family
│       │   ├── TokenStore.java                 # 抽象：code/refresh/access/黑名单 存储
│       │   ├── InMemoryTokenStore.java         # 内存实现（预留 Redis）
│       │   └── TokenRecord.java                # Access/Refresh 记录模型
│       ├── revoke/                             # 能力④：撤销
│       │   └── RevokeEndpoint.java             # POST /oauth2/revoke（client 主动撤销）
│       ├── introspect/                         # 能力⑤：自省
│       │   └── IntrospectEndpoint.java         # POST /oauth2/introspect（RFC 7662）
│       ├── jwks/                               # 能力⑥：公钥分发
│       │   └── JwksEndpoint.java               # GET /.well-known/jwks.json（RS256 公钥）
│       ├── userinfo/                           # 授权服务器侧 userInfo
│       │   └── UserInfoEndpoint.java           # GET /api/userinfo（Bearer JWT 保护）
│       └── common/                             # constant / model / util / config
│           ├── constant/OAuth2Constant.java    # 端点/grant_type/scope/TTL 常量
│           ├── model/Scope.java                # 权限范围枚举
│           ├── model/User.java / SampleUser.java
│           ├── model/SampleClient.java         # sampleThirdPartyApp() / samplePublicClient()
│           ├── config/JwtKeyConfig.java        # RS256 密钥对（演示内存自签）
│           ├── config/SecurityConfig.java      # httpBasic 用户登录 + 路径放行
│           └── util/PkceUtil.java              # S256 challenge/verifier
├── oauth2-resource-server/                     # 资源服务器（验 JWT，呼应 security-demo）
│   └── src/main/java/lan/chaos/oauth2/resource
│       ├── ResourceServerApplication.java
│       ├── api/UserInfoController.java         # GET /api/user/info（Bearer JWT 保护）
│       ├── security/ResourceServerConfig.java  # oauth2ResourceServer(jwt()) 配 jwk-set-uri
│       └── common/constant/ResourceConstant.java
└── oauth2-client/                              # 第三方客户端（驱动端到端，可观察输出）
    └── src/main/java/lan/chaos/oauth2/client
        ├── ClientApplication.java
        ├── flow/OAuth2FlowRunner.java          # ApplicationRunner：授权→换 token→调资源服务器
        └── common/constant/ClientConstant.java
```

> 说明：三个模块各自独立启动（不同端口），整条链路用集成测试 `*Test` + 控制台 `OAuth2FlowRunner` 跑通。

---

## 3. 核心机制设计

### 3.1 客户端注册与管理（`client/`）
- `Client`：`clientId`(UUID，全局唯一)、`clientSecret`(**bcrypt 哈希存储**，绝不明文)、`redirectUris`(Set)、`scopes`(可申请 scope)、`grantTypes`、`pkceRequired`(公共客户端=true)。
- `ClientRegistrationService.register(...)`：演示「审核机制」——返回 `APPROVED`（留 `requireManualReview` 开关注释生产做法）。
- `rotateSecret(clientId)`：生成新 secret、bcrypt 后替换（旧 secret 立失效）。
- `authenticate(clientId, rawSecret)`：bcrypt 比对，供 `/token` `/revoke` `/introspect` 做客户端身份验证。
- seed：启动时注入样例第三方应用 `sampleThirdPartyApp()`（保密客户端，带 secret）与 `samplePublicClient()`（公共客户端，pkceRequired=true，无 secret），供测试/Runner 使用。

### 3.2 授权码流程（`authorize/`，纯 API/控制台形态，无 HTML 页面）
1. `GET /oauth2/authorize?client_id&redirect_uri&scope&state&code_challenge&code_challenge_method=S256[&response_mode=json][&auto_consent=true]`
   - 用户登录：采用 **HTTP Basic** 登录（`/oauth2/authorize` 由 Spring Security 保护），样例用户 `alice`；契合「纯 API」形态，无需登录页。
   - 校验 `client_id` 存在、`redirect_uri` ∈ 注册集合（**强校验防钓鱼**）、`scope` ⊆ 客户端可申请范围（**最小化**）。
   - PKCE：公共客户端必须带 `code_challenge`；计算并暂存 challenge。
2. **授权确认（consent）**：`ConsentService` 生成「客户端申请了哪些 scope」的可读文案（这就是 UI 该渲染的数据，本 demo 用 JSON 返回而非页面）。
   - 默认返回 `consent_id` + scopes 描述 JSON，由调用方再 `POST /oauth2/consent` 决定批准/拒绝（演示完整授权确认机制）。
   - 便捷开关 `auto_consent=true`：演示/测试直通，跳过手动确认直接发码。
3. 通过则生成 `code`（不透明随机串，绑定 `clientId/userId/scope/codeChallenge`，**单次消费**，TTL 10min）。
   - `response_mode=json`（默认，纯 API）：直接返回 `{code, state}`，不依赖浏览器重定向。
   - 否则 `302 redirect_uri?code=...&state=...`。`state` 原样回传（**防 CSRF**）。

### 3.3 Token 模型与签发（`token/`）
- **access_token = JWT**（RS256）：claims = `sub`(userId)、`client_id`、`scope`、`jti`(用于黑名单)、`exp`(默认 30min)、`iat`。`kid` 标密钥版本（轮换友好）。
- **refresh_token = 不透明随机串**：存入 `TokenStore`，元数据 `{ familyId, userId, clientId, scopes, expiry(默认 14d), revoked }`。仅关联 client 可用。
- `TokenEndpoint` 支持两种 `grant_type`：
  - `authorization_code`：验 code（单次消费）+ 验 client（Basic `client_id:client_secret`）+ 验 PKCE `code_verifier`（S256 比对 challenge）；签发 access+refresh。
  - `refresh_token`：**滚动刷新**——签发新 access+新 refresh，旧 refresh 立即标记 `revoked`（family 内旧 token 失效，防泄露滥用）。

### 3.4 Token 生命周期（`TokenStore` 抽象 + 内存实现）
| 阶段 | 机制 | 落点 |
|---|---|---|
| 生成 | JWT(access,短) + opaque(refresh,长)；强随机 `SecureRandom` | `AccessTokenIssuer` / `RefreshTokenService` |
| 使用 | 资源服务器验 JWT 签名+exp+scope；`TokenStore` 黑名单查重 | 资源服务器 / `IntrospectEndpoint` |
| 刷新 | 滚动刷新，旧 refresh 失效 | `RefreshTokenService` |
| 过期 | access 依赖 JWT `exp`；refresh 依赖 `TokenStore` TTL/过期判断 | 验签与 `isExpired` |
| 撤销 | 单点（token→黑名单）/ 批量（by clientId / by userId）/ `/revoke` 端点 | `RevokeEndpoint` + `TokenStore.revoke` |

### 3.5 自省与公钥（`introspect/` + `jwks/`）
- `POST /oauth2/introspect`（RFC 7662）：返回 `{active, scope, client_id, username, exp, ...}`；被撤销/过期/黑名单则返回 `active:false`。供资源服务器懒校验或调试。
- `GET /.well-known/jwks.json`：导出 RS256 **公钥**（JWK），资源服务器配 `jwk-set-uri` 拉取验签（呼应 `security-demo` 的 OAuth2 资源服务器场景）。

### 3.6 安全要点（代码内注释 + 实现）
- HTTPS：所有端点注释「生产强制 HTTPS」，演示可用 http 但打印告警。
- client 身份验证：`/token`、`/revoke` 必须 Basic 校验 `client_secret`（bcrypt 比对）。
- redirect_uri 校验、state 防 CSRF、scope 最小化、PKCE（公共客户端）。
- 审计日志：Token 生成/刷新/撤销打印结构化日志点（不打印 secret/token 明文，仅打 `jti`/`clientId`）。

---

## 4. 接口契约（端点总览）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/oauth2/authorize` | 授权端点（Basic 用户登录；`response_mode=json` 直接回 `{code,state}`，或回 `consent_id`） | 用户 HTTP Basic |
| POST | `/oauth2/consent` | 授权确认：批准/拒绝，批准后发 code | 用户 HTTP Basic |
| POST | `/oauth2/token` | 换/刷 Token | Basic `client_id:client_secret` + PKCE |
| POST | `/oauth2/revoke` | 客户端撤销自身 Token | Basic client 校验 |
| POST | `/oauth2/introspect` | 自省 Token | client 校验（或 bearer） |
| GET | `/.well-known/jwks.json` | 公钥集合 | 公开 |
| POST | `/oauth2/clients` | 客户端注册（演示用） | 无（生产需管理员） |
| GET | `/api/userinfo` | 授权服务器侧的 userInfo | Bearer access_token |

资源服务器：
| GET | `/api/user/info` | 受 Bearer JWT 保护，返回用户信息 | OAuth2 资源服务器（JWK 验签） |

---

## 5. 场景清单（一场景一类，可观察输入→输出）

**auth-server**
1. `ClientRegistrationService` — 注册第三方应用 → 返回 `client_id`/`client_secret`，secret 以 bcrypt 存储；轮换 secret 后旧不可用。
2. `AuthorizationCodeService` — 生成 code → 单次消费成功、二次消费失败（一次性）；超 10min 失效。
3. `AccessTokenIssuer` — 签发 JWT → 解析一致；改 payload 验签失败；过 `exp` 校验失败。
4. `TokenEndpoint.authCodeGrant` — 完整授权码换 Token → 返回 access(JWT)+refresh(opaque)；PKCE verifier 错误则拒。
5. `RefreshTokenService` — 滚动刷新 → 新 access+新 refresh，旧 refresh 调 `/token` 被拒（family 失效）。
6. `RevokeEndpoint` + `TokenStore` — 单点撤销 access → `/introspect` 返回 `active:false`；按 userId 批量撤销 → 该用户全部 token 失效。
7. `IntrospectEndpoint` — 合法 token 返回 `active:true`+scope；黑名单/过期返回 `active:false`。
8. `PkceUtil` — S256：`challenge=BASE64URL(SHA256(verifier))`；错误 verifier 比对失败。
9. `JwksEndpoint` — 导出 JWK；资源服务器据此验签通过。
10. `ConsentService`/`ConsentEndpoint` — 申请 scope 文案；批准→发 code，拒绝→不发。

**resource-server**
11. `ResourceServerConfig` + `UserInfoController` — 合法 Bearer→200 返回 userInfo；缺/错 token→401；scope 不足→403。

**client**
12. `OAuth2FlowRunner` — 程序化跑通：authorize URL → 登录授权 → code → 换 token → 调资源服务器 → 打印「输入→输出」全链路。

---

## 6. 测试策略（第五章）
- 每层 `*Test`（surefire，零外部依赖，内存 `TokenStore`）：覆盖上述 1–11 全部语义，AssertJ 断言。
- `oauth2-auth-server` 用 `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate` 跑端到端授权码→换 Token→自省→刷新→撤销。
- `oauth2-resource-server` 用 `@SpringBootTest` + `MockMvc`，测试内覆写 `JwtDecoder` 为测试 RSA 密钥，签发 JWT 验证 200/401/403（自包含，不拉外部 JWKS）。
- `oauth2-client` 轻量 `*Test` 覆盖 PKCE/URL 构造；全链路三进程由 `OAuth2FlowRunner` 手动演示。
- 覆盖率门禁沿用平台 JaCoCo。

---

## 7. 技术栈与 BOM 选择
- **Spring Boot 2.7.18 + JDK 8**（与 `security-demo` 基线一致）。
- 父：`chaos-parent-jdk8` + `chaos-dependencies-spring-boot2`。
- 依赖：`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-oauth2-resource-server`（资源服务器验签）、`jjwt-api/impl/jackson 0.11.5`（手签 JWT）、`spring-boot-starter-test` + `spring-security-test`。
- **不引入** `spring-security-oauth2-authorization-server`（机制封装过度，违背本仓库教学定位）。
- 密钥：演示用内存 RS256 自签密钥对（`JwtKeyConfig`），代码注释生产走 KMS + JWK 轮换。

---

## 8. README 七段式大纲（实现后填充）
1. 一句话定位 2. 技术栈/入口/触发 3. 快速开始（三模块端口 + 启动命令 + 预期输出）4. 场景一览 5. 场景详解（输入→输出/关键API/坑点）6. 进阶方向（Redis 黑名单、OIDC、设备码、多租户）7. 设计要点（为何手签、为何内存存储、与 security-demo 分工）。

---

## 9. 实现阶段计划（落地顺序）
1. 聚合 `pom` + `oauth2-auth-server` 骨架（启动类、常量、样例数据、RS256 密钥）。
2. `client/` 客户端注册 + `authorize/` 授权码流程 + 用户 Basic 登录 + `consent` API 授权确认（纯 API，无 HTML）。
3. `token/` JWT 签发 + refresh 滚动 + `TokenStore` 内存实现。
4. `revoke/` + `introspect/` + `jwks/` + `userinfo`。
5. `oauth2-resource-server`（JWK 验签受保护接口）。
6. `oauth2-client` 端到端 `OAuth2FlowRunner`。
7. 各模块 `*Test`；README 七段式；根 `README.md` 模块树 + `TECH-INDEX.md` 同步。

---

## 10. 已确认决策（2026-10-24）
- 落地位置：`microservice/oauth2-demo/`（与 `security-demo` 同域）。
- 交互形态：纯 API/控制台演示，无 HTML 页面（登录用 HTTP Basic，授权确认用 `POST /oauth2/consent` API，authorize 默认 `response_mode=json`）。
- BOM：Spring Boot 2.7.18 / JDK 8。
