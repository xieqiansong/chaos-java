package lan.chaos.websockettunnel.tunnel;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lan.chaos.websockettunnel.common.config.TunnelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端验证：Netty WS 客户端 → 内嵌 WS 隧道 → 本地 echo TCP 服务 → 原路返回。
 * 全部走回环地址，零外部依赖；不依赖 Spring 上下文，单测阶段即可稳定跑。
 */
class EmbeddedTunnelServerTest {

    private EventLoopGroup group;
    private Channel echoChannel;
    private EmbeddedTunnelServer server;

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
        if (echoChannel != null) {
            echoChannel.close().syncUninterruptibly();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    @Test
    void forwardsBinaryFramesToTargetTcpAndBack() throws Exception {
        group = new NioEventLoopGroup(2);
        int echoPort = startEchoServer();

        TunnelProperties properties = new TunnelProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setAllowedTargets(Collections.singletonList("127.0.0.1:" + echoPort));
        server = new EmbeddedTunnelServer(properties);
        server.start();

        int wsPort = server.getBoundPort();
        CountDownLatch echoed = new CountDownLatch(1);
        CountDownLatch handshakeComplete = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        URI uri = URI.create("ws://127.0.0.1:" + wsPort + "/forward/127.0.0.1/" + echoPort);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
        WebSocketClientProtocolHandler protocolHandler = new WebSocketClientProtocolHandler(handshaker);

        Bootstrap client = new Bootstrap();
        client.group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpClientCodec(), new HttpObjectAggregator(8192),
                                protocolHandler, new SimpleChannelInboundHandler<BinaryWebSocketFrame>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx,
                                                               BinaryWebSocketFrame frame) {
                                        received.set(frame.content().toString(StandardCharsets.UTF_8));
                                        echoed.countDown();
                                    }
                                }, new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
                                            throws Exception {
                                        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                                            handshakeComplete.countDown();
                                        }
                                        super.userEventTriggered(ctx, evt);
                                    }
                                });
                    }
                });

        Channel ws = client.connect("127.0.0.1", wsPort).sync().channel();
        assertThat(handshakeComplete.await(5, TimeUnit.SECONDS)).isTrue();
        ws.writeAndFlush(new BinaryWebSocketFrame(
                Unpooled.copiedBuffer("hello-tunnel", StandardCharsets.UTF_8))).sync();

        assertThat(echoed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(received.get()).isEqualTo("hello-tunnel");
        ws.close().sync();
    }

    @Test
    void rejectsTargetOutsideWhitelist() throws Exception {
        TunnelProperties properties = new TunnelProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(0);
        properties.setAllowedTargets(Collections.singletonList("127.0.0.1:1"));
        server = new EmbeddedTunnelServer(properties);
        server.start();

        try (Socket socket = new Socket("127.0.0.1", server.getBoundPort())) {
            socket.setSoTimeout(3000);
            String request = "GET /forward/127.0.0.1/3306 HTTP/1.1\r\n"
                    + "Host: 127.0.0.1\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
                    + "Sec-WebSocket-Version: 13\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            assertThat(reader.readLine()).contains("403");
        }
    }

    private int startEchoServer() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                ctx.writeAndFlush(msg.retainedDuplicate());
                            }
                        });
                    }
                });
        echoChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
        return ((InetSocketAddress) echoChannel.localAddress()).getPort();
    }
}
