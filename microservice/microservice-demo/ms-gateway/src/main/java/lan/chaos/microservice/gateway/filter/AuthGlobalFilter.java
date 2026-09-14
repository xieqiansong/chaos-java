package lan.chaos.microservice.gateway.filter;

import lan.chaos.microservice.common.security.constant.SecurityConstants;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * ★★★ P4 网关集中鉴权过滤器（WebFlux GlobalFilter）。
 *
 * <p>统一把关点：所有流量先进网关，这里验签 JWT（本地用共享密钥，无状态、不查库、不调鉴权服务）。
 * 合法则放行并把 {@code Authorization} 透传给下游；不合法/缺失且非白名单 → 直接 401 短路，请求不再往后走。</p>
 *
 * <p>顺序：{@code Ordered.HIGHEST_PRECEDENCE + 10}，早于 Sentinel 限流，
 * 让「未认证」先被拦在限流统计之外（避免对无效请求也计流控）。</p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthDecision decision;

    public AuthGlobalFilter(JwtProvider jwtProvider) {
        this.decision = new AuthDecision(jwtProvider);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1) 白名单直接放行（认证接口本身必须放行，否则死循环）
        if (decision.isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 2) 非白名单必须有合法 access token
        String authHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        if (!decision.authorized(authHeader)) {
            return unauthorized(exchange, "未认证或 token 无效，请先访问 /auth/login 获取令牌");
        }

        // 3) 已认证：默认网关会透传 Authorization 给下游；这里显式重设，确保 header 一定到位
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(SecurityConstants.AUTHORIZATION_HEADER, authHeader)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                escape(message), System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
