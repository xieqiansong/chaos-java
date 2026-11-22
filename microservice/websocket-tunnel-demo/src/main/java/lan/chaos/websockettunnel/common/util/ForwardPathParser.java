package lan.chaos.websockettunnel.common.util;

import lan.chaos.websockettunnel.common.constant.TunnelConstants;
import lan.chaos.websockettunnel.common.model.TargetAddress;

/**
 * 解析 {@code /forward/{host}/{port}}。
 *
 * <p>为什么不用 Spring 路由：隧道服务端是裸 Netty HttpServerCodec + Handshaker，
 * 路径必须从 FullHttpRequest.uri() 里手工取。注意要先把 query string 截掉，
 * 因为网关鉴权把 token 放在 query 上（浏览器 WebSocket 无法自定义 Header）。
 * 解析失败返回 null 由 handler 统一回 400，不抛异常打断 EventLoop。</p>
 */
public final class ForwardPathParser {

    private ForwardPathParser() {
    }

    public static TargetAddress parse(String uri) {
        if (uri == null) {
            return null;
        }
        String path = uri.split("\\?", 2)[0];
        if (!path.startsWith(TunnelConstants.FORWARD_PREFIX)) {
            return null;
        }
        String[] parts = path.substring(TunnelConstants.FORWARD_PREFIX.length()).split("/", -1);
        if (parts.length != 2) {
            return null;
        }
        String host = parts[0].trim();
        if (host.isEmpty()) {
            return null;
        }
        try {
            int port = Integer.parseInt(parts[1]);
            if (port < 1 || port > 65535) {
                return null;
            }
            return new TargetAddress(host, port);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
