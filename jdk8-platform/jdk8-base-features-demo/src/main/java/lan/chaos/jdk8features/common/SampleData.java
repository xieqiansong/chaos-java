package lan.chaos.jdk8features.common;

import lan.chaos.jdk8features.common.model.User;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 共享样例数据工厂：各特性演示统一从这里取数，避免在每个 demo 里重复造数据。
 */
public final class SampleData {

    private SampleData() {
    }

    /** 一组用户：用于 Stream 分组、Optional 取值、方法引用等演示 */
    public static List<User> sampleUsers() {
        return Arrays.asList(
                new User("张三", 28, "北京", LocalDate.of(1996, 5, 1)),
                new User("李四", 34, "上海", LocalDate.of(1990, 3, 12)),
                new User("王五", 22, "北京", LocalDate.of(2002, 8, 20)),
                new User("赵六", 41, "广州", LocalDate.of(1983, 11, 5)),
                new User("钱七", 31, "上海", LocalDate.of(1993, 1, 30))
        );
    }

    /** 一组数字：用于 Lambda/Stream 过滤、方法引用求和等演示 */
    public static List<Integer> sampleNumbers() {
        return Arrays.asList(5, 3, 8, 1, 9, 2, 7);
    }
}
