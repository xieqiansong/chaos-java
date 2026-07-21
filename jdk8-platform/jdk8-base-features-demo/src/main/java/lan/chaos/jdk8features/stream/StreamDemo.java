package lan.chaos.jdk8features.stream;

import lan.chaos.jdk8features.common.SampleData;
import lan.chaos.jdk8features.common.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Stream API（JDK8）：声明式处理集合，支持链式、惰性求值。
 *
 * <p>WHY：JDK7 用 for 循环 + 临时集合做过滤/分组/聚合，噪音大易出错；Stream 用"管道"描述"做什么"而非"怎么做"。
 * 关键 API / 规则：
 * <ul>
 *   <li>中间操作（filter/map/sorted/distinct）惰性，不触发计算；</li>
 *   <li>终止操作（collect/count/reduce）才真正执行并产出结果；</li>
 *   <li>{@code Collectors.groupingBy} 做分组、{@code toMap}/{@code joining} 做收尾；</li>
 *   <li>{@code parallelStream()} 可无改代码地并行化（注意线程安全）。</li>
 * </ul>
 */
public class StreamDemo {

    public static void run() {
        List<User> users = SampleData.sampleUsers();

        // 年龄总和
        int totalAge = users.stream().mapToInt(User::getAge).sum();
        System.out.println("年龄总和: " + totalAge);

        // 按城市分组
        Map<String, List<User>> byCity = users.stream().collect(Collectors.groupingBy(User::getCity));
        System.out.println("按城市分组 key: " + byCity.keySet());

        // 按年龄降序取前 2
        List<User> top2 = users.stream()
                .sorted(Comparator.comparing(User::getAge).reversed())
                .limit(2)
                .collect(Collectors.toList());
        System.out.println("年龄最大2人: " + top2);

        // 城市集合（有序去重）
        Set<String> cities = users.stream()
                .map(User::getCity)
                .collect(Collectors.toCollection(TreeSet::new));
        System.out.println("城市(有序去重): " + cities);
    }
}
