package lan.chaos.microservice.auth.service;

import lan.chaos.microservice.common.security.model.LoginUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 样例用户数据源（demo 用内存 Map，生产替换为 DB/Redis/SSO）。
 *
 * <p>WHY 单独立一个「样例工厂」：满足「每个能力自带样例数据、调用方无需自己准备输入」的规范，
 * 同时把「密码 + 权限」这种敏感/可变的身份数据集中在一处，方便演示不同角色的权限差异。</p>
 */
public final class UserStore {

    /** 用户名 → 凭据（密码 + 身份）。 */
    private static final Map<String, Credential> USERS = new HashMap<>();

    static {
        // admin：全权限
        USERS.put("admin", new Credential("admin123",
                new LoginUser(1L, "admin", new java.util.HashSet<>(Arrays.asList(
                        "user:read", "user:write", "order:read", "order:write")))));
        // guest：只读
        USERS.put("guest", new Credential("guest123",
                new LoginUser(2L, "guest", new java.util.HashSet<>(Collections.singletonList("user:read")))));
    }

    private UserStore() {
    }

    public static Credential findByUsername(String username) {
        return USERS.get(username);
    }

    /** 凭据：密码 + 身份（LoginUser）。 */
    public static class Credential {
        private final String password;
        private final LoginUser loginUser;

        public Credential(String password, LoginUser loginUser) {
            this.password = password;
            this.loginUser = loginUser;
        }

        public String getPassword() {
            return password;
        }

        public LoginUser getLoginUser() {
            return loginUser;
        }
    }

    /** 仅用于演示：列出两个样例账号（README/控制台输出）。 */
    public static List<String> sampleAccounts() {
        return Arrays.asList("admin / admin123（全权限）", "guest / guest123（只读）");
    }
}
