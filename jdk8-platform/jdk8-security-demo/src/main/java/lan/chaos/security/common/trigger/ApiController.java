package lan.chaos.security.common.trigger;

import lan.chaos.security.common.constant.SecurityConstant;
import lan.chaos.security.jwt.JwtService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP 端点（触发外壳，不含知识点）：把上面的认证能力暴露成可 curl 把玩的交互入口。
 *
 * <p>端点：
 * <ul>
 *   <li>GET /api/public —— 免认证。</li>
 *   <li>GET /api/secure —— 需 Basic 认证（alice/secret）。</li>
 *   <li>POST /api/token?user=alice —— 签发 JWT（免认证）。</li>
 *   <li>GET /api/jwt-secure —— 需 {@code Authorization: Bearer <token>}。</li>
 * </ul>
 */
@RestController
public class ApiController {

    @GetMapping(SecurityConstant.PUBLIC)
    public String pub() {
        return "public resource (no auth)";
    }

    @GetMapping(SecurityConstant.SECURE)
    public String secure() {
        String who = SecurityContextHolder.getContext().getAuthentication().getName();
        return "secure resource, user=" + who;
    }

    @PostMapping(SecurityConstant.TOKEN)
    public String token(@RequestParam(defaultValue = "alice") String user) {
        String jwt = JwtService.issue(user, 3600);
        return "Bearer " + jwt;
    }

    @GetMapping(SecurityConstant.JWT_SECURE)
    public String jwtSecure() {
        String who = SecurityContextHolder.getContext().getAuthentication().getName();
        return "jwt-secure resource, subject=" + who;
    }
}
