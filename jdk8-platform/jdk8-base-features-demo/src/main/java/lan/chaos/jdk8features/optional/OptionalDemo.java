package lan.chaos.jdk8features.optional;

import lan.chaos.jdk8features.common.SampleData;
import lan.chaos.jdk8features.common.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Optional（JDK8）：用类型系统显式表达"值可能不存在"，减少散落的 null 判断与 NullPointerException。
 *
 * <p>WHY：方法返回 null 是隐式契约，调用方容易忘记判空；Optional 把"可能为空"写进签名。
 * 关键 API / 规则（生产坑）：
 * <ul>
 *   <li>{@code of} 不允许传 null；{@code ofNullable} 允许；</li>
 *   <li>优先 {@code orElseGet}（惰性）而非 {@code orElse}（急切，默认值总会被计算）；</li>
 *   <li>{@code map/flatMap} 链式安全取值；{@code ifPresent} 仅存在时消费；</li>
 *   <li>避免 {@code get()}（为空直接抛），也不要为了判空而 {@code isPresent()+get()}（那还不如直接判 null）。</li>
 * </ul>
 */
public class OptionalDemo {

    public static void run() {
        List<User> users = SampleData.sampleUsers();

        users.stream().findFirst().ifPresent(u -> System.out.println("第一个用户: " + u.getName()));

        // 链式安全取值：第一个用户的城市，没有则 "未知"
        String city = users.stream().findFirst().map(User::getCity).orElse("未知");
        System.out.println("第一个用户城市: " + city);

        // ofNullable + orElseGet（默认值惰性计算）
        Optional<String> empty = Optional.ofNullable(null);
        String fallback = empty.orElseGet(() -> "默认值(惰性计算)");
        System.out.println("空 Optional 兜底: " + fallback);

        // 空集合 findFirst 返回 empty，不会 NPE
        List<User> none = Collections.emptyList();
        System.out.println("空集合第一个: " + none.stream().findFirst().map(User::getName).orElse("无"));
    }
}
