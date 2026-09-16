# io-netty-demo — Java IO 演进（BIO/NIO/AIO/Netty/零拷贝/Reactor）

一句话定位：IO 模型演进对比：BIO（线程每连接）、NIO（Channel + Buffer + Selector）、AIO（异步回调）、多路复用 Reactor（Acceptor/Handler）、零拷贝、Netty 3.x 收发。

## 场景一览

| 主题 | 代表类 |
|---|---|
| BIO | `io.bio.SocketServer1` / `SocketServer2` / `SocketServerThread` / `SocketClientRequestThread` / `SocketClientDaemon` |
| NIO | `io.nio.NIOServer` / `NIOClient` / `BaseTest` |
| AIO | `io.aio.SocketServer` / `ServerSocketChannelHandle` / `SocketChannelReadHandle` |
| 多路复用(Reactor) | `io.nio.multiplexing.Reactor` / `Acceptor` / `Handler` / `SocketServer1` / `SocketServer2` |
| 零拷贝 | `io.zerocopy.CommonTest` |
| Netty | `io.netty.NettyServer` / `NettyClient` |
| 文件复制 | `FileCopyDemo` / `CommonTest` |

## 快速开始

Server/Client 需**同机成对启动**（先 Server 后 Client）。IDE 运行对应 `main` 即可。

## 设计要点

依赖 Hutool / Lombok（`@Slf4j`）/ `spring-boot-starter-logging` / JBoss Netty 3.x。本模块自原 `jdk8-base` 拆分而来，包名保持不变。
