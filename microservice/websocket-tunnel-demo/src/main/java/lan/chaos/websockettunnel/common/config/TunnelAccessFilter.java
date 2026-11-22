package lan.chaos.websockettunnel.common.config;

import lan.chaos.websockettunnel.common.constant.TunnelConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 只对隧道路径生效的握手鉴权。
 *
 * <p>为什么用 query token 而不是 Authorization 头：浏览器 {@code new WebSocket(url)} 无法自定义 Header，
 * 而 {@code Sec-WebSocket-Protocol} 又要和服务端协商，最省事的演示方案是 query 参数。
 * 生产环境应改用短时效签名 token——query 会落到访问日志和代理日志里。</p>
 */
@Slf4j
@Component
public class TunnelAccessFilter implements GlobalFilter, Ordered {

    private final TunnelProperties properties;

    public TunnelAccessFilter(TunnelProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path == null || !path.startsWith(TunnelConstants.FORWARD_PREFIX)) {
            return chain.filter(exchange);
        }

        String expected = properties.getAccessToken();
        if (expected != null && !expected.isEmpty()) {
            String token = exchange.getRequest().getQueryParams().getFirst(TunnelConstants.TOKEN_PARAM);
            if (!expected.equals(token)) {
                log.warn("隧道握手鉴权失败，path={}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
