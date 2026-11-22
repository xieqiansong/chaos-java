package lan.chaos.websockettunnel.tunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * 目标 TCP → WS 方向。retainedDuplicate 让帧持有底层 ByteBuf 的一份引用，
 * 写出后由 Netty 管线级联释放，原 msg 由 {@link SimpleChannelInboundHandler} 自动释放，引用计数自洽。
 */
@Slf4j
public class TargetTcpHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Channel wsChannel;

    public TargetTcpHandler(Channel wsChannel) {
        this.wsChannel = wsChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (wsChannel.isActive()) {
            wsChannel.writeAndFlush(new BinaryWebSocketFrame(msg.retainedDuplicate()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (wsChannel.isActive()) {
            wsChannel.writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("目标 TCP 连接异常：{}", cause.getMessage());
        if (wsChannel.isActive()) {
            wsChannel.close();
        }
        ctx.close();
    }
}
