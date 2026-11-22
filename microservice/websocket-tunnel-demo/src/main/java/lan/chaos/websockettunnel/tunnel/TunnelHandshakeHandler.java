package lan.chaos.websockettunnel.tunnel;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import lan.chaos.websockettunnel.common.config.TunnelProperties;
import lan.chaos.websockettunnel.common.model.TargetAddress;
import lan.chaos.websockettunnel.common.util.ForwardPathParser;
import lan.chaos.websockettunnel.common.util.TargetPolicy;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 一条 WS 连接 = 一条目标 TCP 连接。负责：HTTP 升级 → 路径解析 → 目标白名单校验 →
 * 异步连目标 TCP → 双向帧转发。
 *
 * <p>每个 handler 实例只服务一条 WS 连接，所以首包缓冲队列不需要加锁。
 * 首包可能在 TCP 三次握手完成前到达，必须暂存并在连接成功后 flush，否则握手后的第一个字节会丢——
 * 这是 TCP over WebSocket 这类隧道最容易踩的坑之一。</p>
 */
@Slf4j
public class TunnelHandshakeHandler extends SimpleChannelInboundHandler<Object> {

    private final TunnelProperties properties;
    private final TargetPolicy targetPolicy;

    private final Queue<ByteBuf> pendingWrites = new ArrayDeque<>();
    private int pendingBytes;

    private Channel wsChannel;
    private Channel tcpChannel;
    private WebSocketServerHandshaker handshaker;

    public TunnelHandshakeHandler(TunnelProperties properties, TargetPolicy targetPolicy) {
        this.properties = properties;
        this.targetPolicy = targetPolicy;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttp(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof BinaryWebSocketFrame) {
            forward(((BinaryWebSocketFrame) msg).content());
        } else if (msg instanceof TextWebSocketFrame) {
            forward(((TextWebSocketFrame) msg).content());
        } else if (msg instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(((PingWebSocketFrame) msg).content().retain()));
        } else if (msg instanceof CloseWebSocketFrame) {
            if (handshaker != null) {
                handshaker.close(ctx.channel(), ((CloseWebSocketFrame) msg).retain());
            }
            closeTcp();
        }
    }

    private void handleHttp(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()
                || !"websocket".equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE))) {
            sendError(ctx.channel(), HttpResponseStatus.BAD_REQUEST);
            return;
        }

        TargetAddress target = ForwardPathParser.parse(request.uri());
        if (target == null) {
            sendError(ctx.channel(), HttpResponseStatus.BAD_REQUEST);
            return;
        }
        // fail-closed：未配置白名单时任何目标都拒绝，避免把网关变成 SSRF 代理
        if (!targetPolicy.isAllowed(target.getHost(), target.getPort())) {
            log.warn("拒绝未授权隧道目标：{}", target);
            sendError(ctx.channel(), HttpResponseStatus.FORBIDDEN);
            return;
        }

        wsChannel = ctx.channel();
        String location = "ws://" + request.headers().get(HttpHeaderNames.HOST);
        WebSocketServerHandshakerFactory factory =
                new WebSocketServerHandshakerFactory(location, null, false, properties.getMaxFrameSize());
        handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            return;
        }
        handshaker.handshake(ctx.channel(), request).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                connectTarget(target);
            } else {
                ctx.close();
            }
        });
    }

    private void connectTarget(TargetAddress target) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(wsChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TargetTcpHandler(wsChannel));
                    }
                });

        bootstrap.connect(target.getHost(), target.getPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.warn("连接目标 TCP 失败 {}：{}", target, future.cause().getMessage());
                wsChannel.writeAndFlush(new TextWebSocketFrame("target connect failed: " + target));
                wsChannel.close();
                return;
            }
            tcpChannel = future.channel();
            log.debug("WS 会话已绑定目标 TCP：{}", target);
            ByteBuf buffered;
            while ((buffered = pendingWrites.poll()) != null) {
                pendingBytes -= buffered.readableBytes();
                tcpChannel.writeAndFlush(buffered);
            }
        });
    }

    private void forward(ByteBuf content) {
        if (!content.isReadable()) {
            return;
        }
        ByteBuf data = content.retainedDuplicate();
        pendingBytes += data.readableBytes();
        if (pendingBytes > properties.getMaxFrameSize() * 2L) {
            data.release();
            log.warn("首包缓冲超过上限，关闭隧道连接");
            closeTcp();
            wsChannel.close();
            return;
        }
        if (tcpChannel != null && tcpChannel.isActive()) {
            pendingBytes -= data.readableBytes();
            tcpChannel.writeAndFlush(data);
        } else {
            pendingWrites.offer(data);
        }
    }

    private void closeTcp() {
        if (tcpChannel != null) {
            tcpChannel.close();
            tcpChannel = null;
        }
    }

    private void releasePending() {
        ByteBuf buffered;
        while ((buffered = pendingWrites.poll()) != null) {
            pendingBytes -= buffered.readableBytes();
            buffered.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        releasePending();
        closeTcp();
        super.channelInactive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        releasePending();
        closeTcp();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("隧道连接异常：{}", cause.getMessage());
        releasePending();
        closeTcp();
        ctx.close();
    }

    private static void sendError(Channel channel, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(status.reasonPhrase(), StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
