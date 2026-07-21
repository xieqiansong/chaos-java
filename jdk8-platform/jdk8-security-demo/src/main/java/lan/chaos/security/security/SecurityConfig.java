package lan.chaos.security.security;

import lan.chaos.security.common.constant.SecurityConstant;
import lan.chaos.security.jwt.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * ★★★ 高频：Spring Security 配置 —— 把「认证 + 授权 + 过滤器」组装起来。
 *
 * <p>痛点：每个接口手写鉴权既重复又易漏。Spring Security 用「过滤器链 + 规则」统一保护端点，
 * 默认拦截一切、白名单放行、未认证返回 401。
 *
 * <p>本配置演示两套并存方案（对比见 README）：
 * <ul>
 *   <li><b>Session/Cookie 之外的 HTTP Basic</b>：{@code /api/secure} 用内存用户 + Basic 认证。</li>
 *   <li><b>无状态 JWT</b>：{@code /api/jwt-secure} 经 {@link JwtAuthFilter} 校验 Bearer Token。</li>
 * </ul>
 *
 * <p>关键 API：{@code SecurityFilterChain} Bean（Spring Boot 2.7 推荐，替代旧 WebSecurityConfigurerAdapter）、
 * {@code authorizeHttpRequests}、{@code httpBasic}、{@code addFilterBefore}、{@code SessionCreationPolicy.STATELESS}。
 *
 * <p>生产坑：
 * <ul>
 *   <li>无状态 JWT 必须 {@code STATELESS}，否则 Security 仍会创建 Session。</li>
 *   <li>密码必须加密存储（{@code {bcrypt}...}），内存演示才用 {@code {noop}}。</li>
 *   <li>关掉 CSRF 仅适合无 Cookie 的无状态 API；有 Session 的 Web 要保留。</li>
 * </ul>
 */
@Configuration
@Order(1)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        new AntPathRequestMatcher(SecurityConstant.PUBLIC),
                        new AntPathRequestMatcher(SecurityConstant.TOKEN)).permitAll()
                .requestMatchers(
                        new AntPathRequestMatcher(SecurityConstant.SECURE),
                        new AntPathRequestMatcher(SecurityConstant.JWT_SECURE)).authenticated()
                .anyRequest().authenticated())
            .httpBasic() // 演示 HTTP Basic 认证（/api/secure）
            .and()
            .addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class); // JWT 校验
        return http.build();
    }

    /** 内存用户（演示用 {noop} 明文；生产用 {bcrypt}）。 */
    @Bean
    public InMemoryUserDetailsManager users() {
        UserDetails alice = User.withUsername("alice")
                .password("{noop}secret").roles("USER").build();
        return new InMemoryUserDetailsManager(alice);
    }
}
