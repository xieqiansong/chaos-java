package lan.chaos.security.jwt;

import io.jsonwebtoken.JwtException;
import lan.chaos.security.common.constant.SecurityConstant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器：拦截请求，从 {@code Authorization: Bearer <token>} 取出 Token，
 * 校验后把「认证信息」写入 Spring Security 上下文，实现无状态鉴权。
 *
 * <p>这是把 JWT 接入 Spring Security 的关键一环：替代了 Session 的「从会话取用户」。
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(SecurityConstant.AUTH_HEADER);
        if (header != null && header.startsWith(SecurityConstant.BEARER)) {
            String token = header.substring(SecurityConstant.BEARER.length());
            try {
                String subject = JwtService.parseSubject(token);
                // 演示：subject 即用户名，角色从 JWT 可取（这里简化，仅演示认证）
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        subject, null, AuthorityUtils.NO_AUTHORITIES);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // 非法/过期 Token：不清上下文，后续授权链会因未认证而返回 401
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
