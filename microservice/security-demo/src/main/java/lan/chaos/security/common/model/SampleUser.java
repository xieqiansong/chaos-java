package lan.chaos.security.common.model;

/**
 * 认证演示用的样例用户。自带工厂，便于测试与演示无需自行拼装。
 */
public class SampleUser {

    private final String username;
    private final String role;

    public SampleUser(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String username() { return username; }
    public String role() { return role; }

    /** 默认样例：普通用户 alice。 */
    public static SampleUser sampleUser() {
        return new SampleUser("alice", "ROLE_USER");
    }

    /** 管理员样例。 */
    public static SampleUser sampleAdmin() {
        return new SampleUser("admin", "ROLE_ADMIN");
    }
}
