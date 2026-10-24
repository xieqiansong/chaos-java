package lan.chaos.oauth2.auth.common.config;

import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 授权服务器的安全链（仅负责「终端用户登录」，即资源拥有者认证）。
 *
 * <p>设计取舍：拆成两条链——
 * 链①（securityMatcher=/oauth2/authorize,/oauth2/consent）启用 HTTP Basic 做用户登录；
 * 链② 其余全部 permitAll，且<b>不挂 Basic 过滤器</b>。
 * 这样可避免全局 Basic 过滤器把客户端在 /oauth2/token 上携带的
 * client_id:client_secret 当成「用户凭据」去校验而误返回 401。
 * 客户端的身份验证由各端点在控制器内手动完成（机制更透明、便于教学）。</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        UserDetails alice = User.withUsername(OAuth2Constant.SAMPLE_USERNAME)
                .password(encoder.encode(OAuth2Constant.SAMPLE_PASSWORD))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(alice);
    }

    /** 链①：仅授权/确认端点要求终端用户 Basic 登录（资源拥有者认证）。 */
    @Bean
    @Order(1)
    public SecurityFilterChain userLoginChain(HttpSecurity http) throws Exception {
        http
                .requestMatcher(new org.springframework.security.web.util.matcher.OrRequestMatcher(
                        new AntPathRequestMatcher(OAuth2Constant.AUTHORIZE_ENDPOINT),
                        new AntPathRequestMatcher(OAuth2Constant.CONSENT_ENDPOINT)))
                .csrf().disable()
                .authorizeRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /** 链②：其余端点（token/revoke/introspect/jwks/userinfo/clients）全部放行，
     * 不挂 Basic 过滤器；客户端/Bearer 验证在控制器内手动完成。 */
    @Bean
    @Order(2)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
