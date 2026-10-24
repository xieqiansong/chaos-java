package lan.chaos.oauth2.auth.common.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 权限范围（scope）。OAuth2 的核心约束之一是「权限最小化」：
 * 第三方应用只能拿到用户明确批准的 scope。
 */
public enum Scope {

    USER_BASIC("user.basic", "读取你的基础资料（昵称、头像）"),
    USER_EMAIL("user.email", "读取你的邮箱地址"),
    USER_PHONE("user.phone", "读取你的手机号");

    private final String value;
    private final String description;

    Scope(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /** 把 "user.basic user.email" 解析成 Scope 集合（按声明顺序去重）。 */
    public static Set<Scope> parse(String spaceSeparated) {
        Set<Scope> result = new LinkedHashSet<>();
        if (spaceSeparated == null || spaceSeparated.trim().isEmpty()) {
            return result;
        }
        for (String part : spaceSeparated.trim().split("\\s+")) {
            Scope s = fromValue(part);
            if (s != null) {
                result.add(s);
            }
        }
        return result;
    }

    /** 把 Scope 集合拼回 "user.basic user.email"（OAuth2 规范用空格分隔）。 */
    public static String join(Set<Scope> scopes) {
        StringBuilder sb = new StringBuilder();
        for (Scope s : scopes) {
            if (sb.length() > 0) {
                sb.append(SCOPE_DELIMITER);
            }
            sb.append(s.value);
        }
        return sb.toString();
    }

    /** requested 是否为 allowed 的子集（权限最小化校验）。 */
    public static boolean isSubset(Set<Scope> requested, Set<Scope> allowed) {
        return allowed.containsAll(requested);
    }

    public static Scope fromValue(String v) {
        return Arrays.stream(values())
                .filter(s -> s.value.equals(v))
                .findFirst()
                .orElse(null);
    }

    private static final String SCOPE_DELIMITER = " ";
}
