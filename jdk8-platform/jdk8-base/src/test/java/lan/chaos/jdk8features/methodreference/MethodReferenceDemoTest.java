package lan.chaos.jdk8features.methodreference;

import lan.chaos.jdk8features.common.SampleData;
import lan.chaos.jdk8features.common.model.User;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodReferenceDemoTest {

    @Test
    void methodReferences() {
        List<User> users = SampleData.sampleUsers();

        List<String> names = users.stream().map(User::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("张三", "李四", "王五", "赵六", "钱七"), names);

        int total = SampleData.sampleNumbers().stream().reduce(0, Integer::sum);
        assertEquals(35, total);
    }
}
