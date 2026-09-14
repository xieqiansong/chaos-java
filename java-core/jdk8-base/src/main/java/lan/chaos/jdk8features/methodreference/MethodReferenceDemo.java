package lan.chaos.jdk8features.methodreference;

import lan.chaos.jdk8features.common.SampleData;
import lan.chaos.jdk8features.common.model.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 方法引用（JDK8）：当 lambda 体"只是调用一个已存在的方法"时，可用 {@code ::} 简写。
 *
 * <p>WHY：让代码更紧凑、意图更清晰（直接说"用这个方法"，而非"写一段 lambda 去调它"）。
 * 四种形式：
 * <ul>
 *   <li>静态方法：{@code ClassName::staticMethod}</li>
 *   <li>实例方法（任意对象）：{@code ClassName::instanceMethod}</li>
 *   <li>特定对象实例方法：{@code instance::method}</li>
 *   <li>构造器：{@code ClassName::new}</li>
 * </ul>
 */
public class MethodReferenceDemo {

    public static void run() {
        List<User> users = SampleData.sampleUsers();

        // 1) 实例方法引用：User::getName 等价于 u -> u.getName()
        List<String> names = users.stream().map(User::getName).collect(Collectors.toList());
        System.out.println("名字(方法引用): " + names);

        // 2) 静态方法引用：Integer::sum 等价于 (a,b) -> Integer.sum(a,b)
        int total = SampleData.sampleNumbers().stream().reduce(0, Integer::sum);
        System.out.println("求和(Integer::sum): " + total);

        // 3) 构造器引用：用每个字符串 new 一个 String（演示形态）
        List<String> copied = names.stream().map(String::new).collect(Collectors.toList());
        System.out.println("构造器引用(复制字符串): " + copied);
    }
}
