# websocket-tunnel-demo — Spring Cloud Gateway 集成 TCP over WebSocket 隧道

学什么：把 `chaos-tcp-over-websockets` 的「服务端能力」以**代码内嵌**方式放进 Spring Cloud Gateway，同一 JVM 里再起一个只绑回环地址的 Netty WS 端口（7002），由网关用 `ws://` 路由把 `/forward/**` 代理过去，最终桥接到目标 TCP。覆盖 **内嵌 Netty 服务的异步生命周期、Gateway WebSocket 路由、首包缓冲、目标白名单（防 SSRF）、握手鉴权** 五个能力点。

- 基础包：`lan.chaos.websockettunnel`
- 技术栈：Spring Boot 2.7.18 + Spring Cloud Gateway 2021.0.9（WebFlux / Reactor Netty）+ Netty（由 Boot BOM 管理）+ JDK 8
- 入口类：`lan.chaos.websockettunnel.WebsocketTunnelApplication`（网关 18080）
- 验证入口：`src/test/` 下 4 个 `*Test`，其中 `EmbeddedTunnelServerTest` 用本地 echo TCP 服务做端到端转发，零外部依赖

> 使用频率标注：`★★★ 高频`／`★★☆ 中频`／`◆ 基础`。

## 目录结构

```
websocket-tunnel-demo/
├── pom.xml
├── src/main/resources/application.yml
└── src/main/java/lan/chaos/websockettunnel/
    ├── WebsocketTunnelApplication.java              # 启动类（Gateway 本身）
    ├── common/
    │   ├── config/
    │   │   ├── TunnelProperties.java                # 配置属性：端口/白名单/token/帧大小 ◆
    │   │   ├── EmbeddedTunnelConfiguration.java     # 显式 @Bean 装配，避开原项目启动类扫描 ◆
    │   │   └── TunnelAccessFilter.java              # 网关侧握手鉴权 ★★☆
    │   ├── constant/TunnelConstants.java            # 路径前缀、参数名 ◆
    │   ├── model/TargetAddress.java                 # 目标地址模型 + sample() ◆
    │   └── util/
    │       ├── ForwardPathParser.java               # /forward/{host}/{port} 解析 ★★☆
    │       └── TargetPolicy.java                    # 目标白名单，fail-closed ★★★
    └── tunnel/
        ├── EmbeddedTunnelServer.java                # SmartLifecycle 托管的内嵌 WS 服务 ★★★
        ├── TunnelHandshakeHandler.java              # 握手 + 首包缓冲 + 双向帧转发 ★★★
        └── TargetTcpHandler.java                    # 目标 TCP → WS 回写 ★★☆
```

> 设计要点：**能力即顶层包**（`tunnel/`），配置/常量/模型/工具收进 `common/`。网关鉴权是接入层关注点，放 `common/config/`。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [内嵌隧道生命周期 EmbeddedTunnelServer](#1-内嵌隧道生命周期-embeddedtunnelserver) → 只等 bind、不停 `closeFuture().sync()`；stop 先关 Channel 再释放线程组
- [目标白名单 TargetPolicy](#2-目标白名单-targetpolicy) → fail-closed，杜绝隧道变成 SSRF 跳板

`★★☆ 中频`
- [Gateway WebSocket 路由](#3-gateway-websocket-路由与握手鉴权) → `ws://` scheme + 路径原样透传 + query token
- [首包缓冲与双向帧转发](#4-首包缓冲与双向帧转发) → TCP 未连上时暂存 ByteBuf，连上后 flush

`◆ 基础模块`
- [ForwardPathParser / TunnelProperties](#5-路径解析与配置收敛-基础) → 路径解析与配置收敛

---

## 快速开始

```bash
# 1) 单测（4 个 *Test，纯本地端口，无外部依赖）
mvn -pl microservice/websocket-tunnel-demo -am test

# 2) 打包并启动（网关 18080，内嵌隧道 127.0.0.1:7002）
mvn -pl microservice/websocket-tunnel-demo -am package -DskipTests
java -jar microservice/websocket-tunnel-demo/target/websocket-tunnel-demo-1.0-SNAPSHOT.jar
```

验证握手（期望 `101 Switching Protocols`；白名单未放行的目标会得到 `403`）：

```bash
curl -i "http://127.0.0.1:18080/forward/127.0.0.1/3306?token=demo-token" \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" -H "Sec-WebSocket-Version: 13"
```

验证数据透传：先启动一个本地 echo TCP 服务（如 `nc -lk 20001`），把它的地址加进 `tunnel.embedded.allowed-targets`，再用任意 WS 客户端连接
`ws://127.0.0.1:18080/forward/127.0.0.1/20001?token=demo-token` 发二进制帧，应原样收到回显。
端到端行为已被 `EmbeddedTunnelServerTest#forwardsBinaryFramesToTargetTcpAndBack` 自动覆盖，无需手工搭服务。

---

### 1. 内嵌隧道生命周期 EmbeddedTunnelServer `★★★`

原项目 `WebsocketServer.start()` 用 `closeFuture().sync()` 永久阻塞，且 `close()` 只递减 EventLoopGroup 引用计数、不关闭监听 Channel——直接塞进网关会挂住启动、停服时端口不释放。本模块改成 `SmartLifecycle`：

- `start()`：`bootstrap.bind(host, port).sync()` 只等 **bind** 完成（毫秒级），不等待连接关闭；
- `stop()`：先 `serverChannel.close()`，再 `bossGroup/workerGroup.shutdownGracefully()`，两个动作严格各一次；
- `getPhase()` 设为 `HIGHEST_PRECEDENCE + 100`，保证网关开始转发前 7002 已可连；
- 用 NIO 而非 Epoll，避免 native transport 在非 Linux 环境的兼容问题。

验证：`EmbeddedTunnelServerTest` 用端口 0 启动并拿实际端口，分别验证正常转发与关闭后端口释放。

### 2. 目标白名单 TargetPolicy `★★★`

隧道的 `/forward/{host}/{port}` 目标完全由客户端指定，暴露到网关后就是现成的 SSRF 跳板。`TargetPolicy` 默认 **fail-closed**：规则为空一律拒绝；规则支持 `host:port`（精确）与 `host`（该主机任意端口），匹配时忽略大小写与首尾空白。

验证：`TargetPolicyTest` 覆盖精确规则、主机通配、空规则拒绝；`EmbeddedTunnelServerTest#rejectsTargetOutsideWhitelist` 用裸 HTTP 升级请求断言 403。

### 3. Gateway WebSocket 路由与握手鉴权 `★★☆`

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: tcp-tunnel-ws
          uri: ws://127.0.0.1:7002
          predicates:
            - Path=/forward/**
```

三个关键点：

1. **scheme 必须是 `ws://`**——只有 `WebSocketRoutingFilter` 会处理升级与头透传；
2. **不要加 `StripPrefix`**——隧道端按原始 `/forward/{host}/{port}` 解析，改路径就 400；
3. **鉴权走 query token**——浏览器 `new WebSocket(url)` 不能自定义 Header，`TunnelAccessFilter` 只对隧道路径校验 `?token=`；生产应换短时效签名 token（query 会进访问日志）。

验证：`TunnelAccessFilterTest` 断言无 token→401、合法 token→放行、非隧道路径不受影响。

### 4. 首包缓冲与双向帧转发 `★★☆`

WS 握手成功不代表目标 TCP 已连上（三次握手尚未完成）。`TunnelHandshakeHandler` 为每条连接维护一个 `pendingWrites` 队列：TCP 未就绪时把 `retainedDuplicate()` 的 ByteBuf 暂存，连接成功后按序 flush；超过 `maxFrameSize * 2` 直接断开，避免慢连接打爆内存。

二进制帧与文本帧都按字节透传；Ping 回 Pong；目标 TCP 断开时回 Close 帧并释放本端连接。`TargetTcpHandler` 负责反方向回写。引用计数链路：`retainedDuplicate()` 增加一份引用，Netty 写完后级联释放，原帧由 `SimpleChannelInboundHandler` 自动释放。

### 5. 路径解析与配置收敛（基础）`◆`

`ForwardPathParser.parse()` 从 URI 里剥掉 query 再 split，非法端口/缺段/非 `/forward/` 前缀统一返回 `null`，由 handler 回 400 而不抛异常打断 EventLoop。`TunnelProperties` 把端口、host、帧大小、超时、token、白名单集中成 `@ConfigurationProperties`，改部署无需动代码。

验证：`ForwardPathParserTest` 覆盖正常解析、带 query、非法形态。

---

## 进阶方向（生产化考量，未实现）

- **目标白名单升级到服务发现**：静态 `host:port` 不适合云环境，改为按服务名 + 环境标签动态解析，并限制可访问端口段。
- **会话多路复用**：当前一连接一 TCP；`chaos-tcp-over-websockets-multiplexed` 的 sessionId 协议可复用一批 WS 连接承载多 TCP，但需把 `length` 字段真正用于粘包校验（原实现只写不读）。
- **鉴权强化**：query token 换 mTLS 或短时效 HMAC 签名；网关访问日志对 token 脱敏。
- **连接级限流与隔离**：每条隧道限制帧速率/并发数，配合 Sentinel 网关限流；慢连接单独隔离 EventLoop。
- **Boot 3 升级**：`javax.annotation` → `jakarta.annotation`，并重新核对 Reactor Netty 与 Netty 版本对齐。
- **可观测性**：暴露隧道连接数、首包排队长度、目标连接失败率的 Micrometer 指标。

## 设计要点

- **为什么是「同 JVM + 独立端口」**：把 Netty handler 直接挂到 Reactor Netty pipeline 要重写 HTTP 编解码链路；独立进程又多一套部署。同 JVM 起一个只绑 `127.0.0.1` 的 Netty 端口、由网关代理过去，既拿到单进程部署，又完全避开 Netty 版本/EventLoop/Profile 冲突。
- **依赖版本**：刻意不引原项目的 `netty-all:4.1.115.Final`（聚合包与 Reactor Netty 的拆分 artifact 混用易冲突），改为按需引 `netty-codec-http`/`netty-handler`/`netty-transport`，版本由 Spring Boot BOM 与 Reactor Netty 统一。
- **不扫原项目启动类**：显式 `@Bean` 装配隧道服务。`TcpOverWebsocketsApp` 带 `@SpringBootApplication` 且无条件注册依赖 `TunnelLauncher` 的 `ApplicationRunner`，被宿主扫到会在缺 profile 时启动失败。
- **测试分层**：解析/白名单/鉴权为纯单测；隧道转发用本地 echo 服务做端到端单测；需要真实 Nacos/Sentinel 的行为留待 `verify` 阶段（`*IT`），不污染 `mvn test`。
- **频率结论**：生命周期与白名单是接入这类工具的必答题；首包缓冲是协议实现里最容易漏的细节。
