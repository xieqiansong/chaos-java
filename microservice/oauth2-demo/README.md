# OAuth2.0 授权服务 + Token 生命周期管理

> 手写一个最小可用的 OAuth2.0 **授权服务器（IdP）**，覆盖「第三方应用接入 + 授权码流程 + Token 全生命周期」。
> 本 demo 是 `security-demo`（资源服务器侧）的互补一半：它负责「**发** Token + **管**生命周期」，并配套一个资源服务器与第三方客户端把整条链路跑通。
> 详见开发文档 [`DESIGN.md`](DESIGN.md)。

---

## 1. 一句话定位

不引 `spring-authorization-server`，用 Spring Boot + jjwt **手写**授权服务器，讲清 OAuth2 授权码模式与 Token 生命周期的每一个机制环节。

## 2. 技术栈 / 入口 / 触发

- **Spring Boot 2.7.18 / JDK 8**；`jjwt 0.11.5`（手签 RS256 JWT）；资源服务器用 `spring-boot-starter-oauth2-resource-server`。
- 三个独立进程（形态二多模块）：
  | 模块 | 启动类 | 默认端口 | 角色 |
  |---|---|---|---|
  | `oauth2-auth-server` | `AuthServerApplication` | 18081 | 授权服务器（IdP） |
  | `oauth2-resource-server` | `ResourceServerApplication` | 18082 | 受保护资源（JWK 验签） |
  | `oauth2-client` | `ClientApplication` | 18083 | 第三方客户端（端到端演示） |
- 触发：依次启动上述三个应用；客户端启动后由 `OAuth2FlowRunner` 自动跑通全链路并打印「输入→输出」。

## 3. 快速开始

```bash
# 在 chaos-java 根目录
mvn -o -pl microservice/oauth2-demo/oauth2-auth-server -am spring-boot:run      # 终端1
mvn -o -pl microservice/oauth2-demo/oauth2-resource-server -am spring-boot:run  # 终端2
mvn -o -pl microservice/oauth2-demo/oauth2-client -am spring-boot:run          # 终端3（自动跑流程）
```

内置样例身份（无需注册即可演示）：
- 终端用户（资源拥有者）：`alice` / `secret`（HTTP Basic 登录授权端点）
- 保密客户端：`demo_client_01` / `demo_secret_01`（带 secret，授权码 + 刷新）
- 公共客户端（PKCE）：`demo_public_client_01`（无 secret，强制 PKCE）

预期：客户端日志依次打印「授权→换 Token→访问资源→刷新→撤销后 401」。也可直接 curl：
```bash
# 1) 取授权码（Basic 登录 alice，auto_consent 直通）
curl -u alice:secret 'http://localhost:18081/oauth2/authorize?client_id=demo_client_01&redirect_uri=http://127.0.0.1:18083/cb&scope=user.basic&response_mode=json&auto_consent=true'
# 2) 换 Token（Basic 客户端身份）
curl -u demo_client_01:demo_secret_01 -d 'grant_type=authorization_code&code=<上一步code>&redirect_uri=http://127.0.0.1:18083/cb' http://localhost:18081/oauth2/token
```

## 4. 场景一览

| # | 场景 | 落点 |
|---|---|---|
| 1 | 客户端注册 / 审核 / 密钥轮换 | `client/ClientRegistrationService` |
| 2 | 授权码：单次消费、短 TTL | `authorize/AuthorizationCodeService` |
| 3 | JWT access_token 签发 / 验签 / 过期 | `token/AccessTokenIssuer` |
| 4 | 授权码换 Token（含 PKCE 校验） | `token/TokenEndpoint` |
| 5 | refresh_token 滚动刷新（旧失效） | `token/RefreshTokenService` + `TokenEndpoint` |
| 6 | 撤销（黑名单）+ 批量撤销 | `revoke/RevokeEndpoint` + `token/InMemoryTokenStore` |
| 7 | 自省（RFC 7662） | `introspect/IntrospectEndpoint` |
| 8 | JWK 公钥分发 | `jwks/JwksEndpoint` |
| 9 | 资源服务器 JWK 验签受保护接口 | `resource/security/ResourceServerConfig` |
| 10 | 第三方客户端端到端 | `client/flow/OAuth2FlowRunner` |

## 5. 场景详解（关键 API / 坑点）

- **授权码 + PKCE**：公共客户端必须带 `code_challenge(S256)`；换 Token 时验 `code_verifier`。密钥轮换：旧 `client_secret` 立即失效（bcrypt 哈希比对）。
- **Token 模型**：access = JWT（RS256，`jti/exp/scope/client_id/sub`，便于无状态验签）；refresh = 不透明随机串（落库，便于撤销）。
- **滚动刷新**：每次刷新签发新 access + 新 refresh，旧 refresh 立即进入 `revoked`；familyId 串联令牌族，便于整族失效（如用户改密）。
- **撤销与 JWT 的矛盾**：JWT 自包含无法即时失效 → 用 `jti` 黑名单（`InMemoryTokenStore.revokeAccess` / `revokeByUser` / `revokeByClient`）。生产可换 Redis（TTL 自然过期 + 黑名单）。
- **安全要点**：`redirect_uri` 强校验防钓鱼、`state` 防 CSRF、scope 最小化、客户端 `client_secret` bcrypt 校验、生产全程 HTTPS。

## 6. 进阶方向（未实现）

- 用 Redis 替代内存存储（分布式黑名单 / 多实例共享 refresh）。
- 完整 OpenID Connect（id_token、userinfo 标准化 claims）。
- 设备码模式（Device Code）、动态 scope 审批 UI。
- 多租户 / 密钥 `kid` 轮换（KMS 托管私钥）。

## 7. 设计要点（为什么这么写）

- **手写而非引库**：本仓库定位「讲清机制」，故不依赖 `spring-authorization-server`，把授权码、Token、刷新、撤销逐一显式实现。
- **客户端身份验证手动做**：`/token`、`/revoke`、`/introspect` 在控制器内解析 Basic 头并 bcrypt 比对，避开 Spring Security 全局 Basic 过滤器与用户登录混淆（安全链见 `SecurityConfig` 双链设计）。
- **内存存储**：零中间件即可跑测试（`*Test` 全绿），生产只需替换 `TokenStore` 实现。
- **与 `security-demo` 分工**：`security-demo` 是「资源服务器消费外部 IdP」，本 demo 是「那个 IdP 本身」，二者通过 `JWK Set URI` 衔接。
