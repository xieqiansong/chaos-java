package lan.chaos.jdk11features.stream;

import lan.chaos.jdk11features.common.SampleData;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamDemoTest {

    @Test
    void takeWhileDropWhileOfNullable() {
        List<Integer> nums = SampleData.sampleNumbers(); // 1..10

        List<Integer> taken = nums.stream().takeWhile(n -> n < 5).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3, 4), taken);

        List<Integer> dropped = nums.stream().dropWhile(n -> n < 5).collect(Collectors.toList());
        assertEquals(Arrays.asList(5, 6, 7, 8, 9, 10), dropped);

        assertEquals(0, Stream.ofNullable(null).count());
        assertEquals(1, Stream.ofNullable("7").count());
    }
}
