# jdk8-security-demo  ★ A 类（认证与授权）

一句话定位：用一个样例用户 `alice`，演示三类认证授权方案——**Spring Security 基础防护、JWT 无状态 Token、OAuth2 资源服务器（外部 IdP）**，覆盖「白名单放行 / Basic 认证 / Bearer JWT 校验 / OAuth2 资源服务器」与 Session-Cookie vs Token 对比。Web 层用 MockMvc 断言（零外部依赖），OAuth2 需外部 IdP（提供 docker-compose 参考）。

- 基础包：`lan.chaos.security`
- 技术栈：Spring Boot 2.7.18 + spring-boot-starter-security + jjwt 0.11.5 + spring-boot-starter-oauth2-resource-server
- 验证入口：`src/test/.../*Test`（JWT 签发/过期/篡改；Web 端点 401/200 行为）
- 启动类：`SecurityApplication`（四个 HTTP 端点可 curl 把玩）

> 使用频率：`★★★ 高频`。

## 目录结构

```
jdk8-security-demo/
├── pom.xml
├── docker-compose.yml                 # 可选：Keycloak 授权服务器（OAuth2 演示用）
└── src/main/java/lan/chaos/security
    ├── SecurityApplication.java        # 启动类
    ├── common/
    │   ├── constant/SecurityConstant.java   # 路径/请求头/密钥等常量
    │   ├── model/SampleUser.java            # 样例用户（sampleUser/sampleAdmin）
    │   └── trigger/ApiController.java       # HTTP 端点（触发外壳）
    ├── jwt/JwtService.java             # JWT 签发/解析 ★★★
    ├── jwt/JwtAuthFilter.java          # Bearer Token 校验过滤器 ★★★
    └── security/
        ├── SecurityConfig.java         # Spring Security 主链（Basic + JWT 共存）★★★
        └── OAuth2ResourceServerConfig.java  # OAuth2 资源服务器（配了 IdP 才启用）★★★
```

## 场景一览

`★★★ 高频`
- [Spring Security 基础防护](#1-spring-security-基础防护) → 过滤器链、白名单、Basic 认证、无状态
- [JWT 无状态 Token](#2-jwt-无状态-token) → 签发/校验/过滤器接入
- [OAuth2 资源服务器](#3-oauth2-资源服务器外部-idp) → 授权码模式、把认证外包给 IdP

---

### 1. Spring Security 基础防护 `★★★`

痛点：每个接口手写鉴权既重复又易漏。Spring Security 用「过滤器链 + 规则」统一保护，默认拦截一切、白名单放行、未认证返回 401。
关键 API：`SecurityFilterChain` Bean（2.7 推荐，替代旧 `WebSecurityConfigurerAdapter`）、`authorizeHttpRequests`、`httpBasic`、`SessionCreationPolicy.STATELESS`。
生产坑：无状态必须 `STATELESS`（否则仍建 Session）；密码应 `{bcrypt}` 加密（演示才 `{noop}`）；有 Session 的 Web 别乱关 CSRF。
验证：`ApiControllerTest.secureEndpointRequiresAuth`（无认证 401、Basic 200 且带回用户名）。

---

### 2. JWT 无状态 Token `★★★`

痛点：传统 Session 需服务端存登录态、集群要共享（Redis），跨域/移动端不友好。JWT 把声明自包含进签名 Token，服务端**无状态**校验即可。
关键 API（jjwt）：`Jwts.builder().setSubject().setExpiration().signWith(key,HS256).compact()` 与 `parserBuilder().setSigningKey().build().parseClaimsJws(token)`。
本 demo：`JwtService` 签发/解析；`JwtAuthFilter` 从 `Authorization: Bearer <token>` 取 Token 写 SecurityContext；`/api/jwt-secure` 经它保护。
生产坑：密钥够强且放 KMS；JWT 无法主动吊销（短过期+刷新令牌/黑名单）；payload 非加密别放敏感；固定 HS256 防算法混淆。
验证：`JwtServiceTest`（签发解析一致、过期抛异常、篡改抛异常）；`ApiControllerTest.tokenIssuanceAndJwtSecure`（签发→Bearer 访问 200、非法 Token 401）。

---

### 3. OAuth2 资源服务器（外部 IdP） `★★★`

痛点：自己管登录/密钥轮换/注销成本高。生产更常见把认证外包给 IdP（Keycloak/Auth0/微信），本服务只做**资源服务器**：用 IdP 公钥校验其签发的 JWT。
关键 API：`spring-boot-starter-oauth2-resource-server` + `oauth2ResourceServer(o -> o.jwt())`，配 `jwk-set-uri` 指向 IdP 的 JWK 集合。
**默认不启用**：`OAuth2ResourceServerConfig` 用 `@ConditionalOnProperty(jwk-set-uri)` + `@Order(0)`，仅当配置了 IdP 才激活并优先于本地链。不带 IdP 时 demo 仍用本地 JWT/Basic 跑通。
授权码模式流程：客户端 → 重定向 IdP 登录授权 → 拿 code → 换 access_token(JWT) → 带 `Bearer` 访问本服务 → 本服务用 IdP 公钥验签。
生产坑：必须 HTTPS；公共客户端开 PKCE；权限从 JWT claims 取；IdP 不可达要有 JWK 缓存降级。
验证：本方案需外部 IdP，未写集成测试；提供 `docker-compose.yml`（Keycloak）与 `application.yml` 配置示例，按 README「如何运行」切换。

---

## 如何运行

```bash
# 1) 跑测试（MockMvc，零外部依赖，任何环境直接过）
mvn -pl jdk8-security-demo test

# 2) 启动应用，用 curl 把玩四个端点：
mvn -pl jdk8-security-demo spring-boot:run
curl localhost:8080/api/public
curl -u alice:secret localhost:8080/api/secure
curl -X POST "localhost:8080/api/token?user=alice"        # 返回 "Bearer <jwt>"
curl -H "Authorization: Bearer <上一步jwt>" localhost:8080/api/jwt-secure

# 3) （可选）切到 OAuth2 资源服务器：先 docker compose up -d 起 Keycloak，
#    再在 application.yml 配 jwk-set-uri（见 docker-compose.yml 注释），重启即可。
```

预期（测试已断言）：
```
GET /api/public        -> 200
GET /api/secure        -> 401；带 Basic alice:secret -> 200 且 "user=alice"
POST /api/token        -> 200 返回 "Bearer <jwt>"
GET /api/jwt-secure    -> 带合法 Bearer 200（"subject=alice"）；非法 Token 401
```

## 进阶方向（生产考量）

- `◆` **Session vs Token 对比**：Session/Cookie 简单但需服务端状态（集群靠 Redis）；JWT 无状态但难吊销。按「是否多端/微服务」选型。
- `◆` **刷新令牌**：access_token 短过期，refresh_token 换发，兼顾安全与体验。
- `◆` **方法级授权**：`@PreAuthorize("hasRole('ADMIN')")` 做更细粒度控制。
- `◆` **OAuth2 授权码 + PKCE**：公共客户端（SPA/移动端）必开，防授权码拦截。
- `◆` **密码存储**：`{bcrypt}`/`Argon2`，绝明文（见 `jdk8-crypto-demo`）。

## 设计要点

- **同一样例用户**：`alice` 贯穿 Basic/JWT/OAuth2，对比公平、可断言。
- **能力即顶层包**：`jwt/security` 各自聚焦；`trigger` 仅做 HTTP 把玩外壳。
- **外部依赖最小化**：JWT/Basic 零外部依赖直接跑；OAuth2 用条件化 Bean，无 IdP 也不阻塞 demo 与测试。
- **频率结论**：Spring Security 是基础；JWT 是无状态 API 主流；OAuth2 资源服务器是接入企业 IdP 的标准姿势。
