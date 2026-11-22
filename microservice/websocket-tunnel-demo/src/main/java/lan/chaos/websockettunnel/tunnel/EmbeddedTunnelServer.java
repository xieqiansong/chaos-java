package lan.chaos.websockettunnel.tunnel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lan.chaos.websockettunnel.common.config.TunnelProperties;
import lan.chaos.websockettunnel.common.util.TargetPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;

import java.net.InetSocketAddress;

/**
 * 同 JVM 内嵌的 Netty WebSocket 服务端，由 Spring 生命周期托管。
 *
 * <p>为什么不复用原项目的 {@code WebsocketServer.start()}：它阻塞在 {@code closeFuture().sync()}，
 * 直接放进启动流程会挂住整个网关；而且它的 {@code close()} 只递减 EventLoopGroup 引用计数、
 * 不关闭监听 Channel，停止时端口不会释放。这里改为：start 只等 bind 完成（毫秒级），
 * stop 先关 Channel 再 shutdownGracefully，两个动作严格各做一次。</p>
 *
 * <p>线程模型：boss 1 线程，worker = CPU×2；刻意用 NIO 而非 Epoll，避免 native transport
 * 在非 Linux 环境的兼容问题。网关的 Reactor Netty 有自己独立的 LoopResources，两者不共享——
 * 端口隔离已经解决了冲突，没有必要强行共享 EventLoop。</p>
 */
@Slf4j
public class EmbeddedTunnelServer implements SmartLifecycle {

    private final TunnelProperties properties;
    private final TargetPolicy targetPolicy;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running;

    public EmbeddedTunnelServer(TunnelProperties properties) {
        this.properties = properties;
        this.targetPolicy = new TargetPolicy(properties.getAllowedTargets());
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(threads);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(properties.getMaxFrameSize()),
                                    new TunnelHandshakeHandler(properties, targetPolicy));
                        }
                    });

            // 只等 bind，不等 closeFuture——这是与隧道原实现最关键的差异
            ChannelFuture future = bootstrap.bind(properties.getHost(), properties.getPort()).sync();
            serverChannel = future.channel();
            running = true;
            log.info("内嵌 WS 隧道已启动，ws://{}:{}/forward/{{host}}/{{port}}",
                    properties.getHost(), getBoundPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("内嵌 WS 隧道启动被中断", e);
        } catch (RuntimeException e) {
            shutdownGroups();
            throw e;
        }
    }

    /** 取实际绑定端口；配 0 时由系统分配，测试与日志都用这个方法。 */
    public int getBoundPort() {
        if (serverChannel == null) {
            throw new IllegalStateException("隧道服务尚未启动");
        }
        return ((InetSocketAddress) serverChannel.localAddress()).getPort();
    }

    @Override
    public void stop() {
        doStop(null);
    }

    @Override
    public void stop(Runnable callback) {
        doStop(callback);
    }

    private void doStop(Runnable callback) {
        if (!running && serverChannel == null) {
            if (callback != null) {
                callback.run();
            }
            return;
        }
        try {
            if (serverChannel != null) {
                serverChannel.close().syncUninterruptibly();
                serverChannel = null;
            }
            running = false;
        } finally {
            shutdownGroups();
            if (callback != null) {
                callback.run();
            }
        }
    }

    private void shutdownGroups() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.isEnabled();
    }

    /** 早于 Web 容器启动，保证网关开始转发时 7002 已经可连。 */
    @Override
    public int getPhase() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

}
