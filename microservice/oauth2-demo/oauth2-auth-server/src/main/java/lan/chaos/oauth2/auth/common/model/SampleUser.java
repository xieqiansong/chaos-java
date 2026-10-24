package lan.chaos.oauth2.auth.common.model;

/**
 * 样例终端用户工厂。演示「一场景自带样例数据」：无需调用方自己准备输入。
 */
public final class SampleUser {

    private SampleUser() {
    }

    public static User sampleUser(String username) {
        User user = new User();
        user.setId("uid_" + username);
        user.setUsername(username);
        user.setName("用户" + username);
        user.setEmail(username + "@example.com");
        return user;
    }
}
