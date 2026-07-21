package lan.chaos.jdk8features.stream;

import lan.chaos.jdk8features.common.SampleData;
import lan.chaos.jdk8features.common.model.User;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamDemoTest {

    @Test
    void groupByCityAndTop2() {
        List<User> users = SampleData.sampleUsers();

        int totalAge = users.stream().mapToInt(User::getAge).sum();
        assertEquals(156, totalAge);

        List<String> cities = new ArrayList<>(
                users.stream().map(User::getCity).collect(Collectors.toCollection(TreeSet::new)));
        assertEquals(Arrays.asList("上海", "北京", "广州"), cities);

        List<User> top2 = users.stream()
                .sorted(Comparator.comparing(User::getAge).reversed())
                .limit(2)
                .collect(Collectors.toList());
        assertEquals(41, top2.get(0).getAge());
        assertEquals(34, top2.get(1).getAge());
    }
}
