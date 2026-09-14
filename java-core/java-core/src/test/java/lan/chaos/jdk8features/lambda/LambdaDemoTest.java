package lan.chaos.jdk8features.lambda;

import lan.chaos.jdk8features.common.SampleData;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaDemoTest {

    @Test
    void filterEvenAndGreaterThan5() {
        List<Integer> nums = SampleData.sampleNumbers();

        List<Integer> evens = nums.stream().filter(n -> n % 2 == 0).collect(Collectors.toList());
        assertEquals(Arrays.asList(8, 2), evens);

        List<Integer> big = nums.stream().filter(n -> n > 5).collect(Collectors.toList());
        assertEquals(Arrays.asList(8, 9, 7), big);
    }
}
