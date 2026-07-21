package lan.chaos.jdk11features.toarray;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToArrayDemoTest {

    @Test
    void toArrayWithIntFunction() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] arr = list.toArray(String[]::new);
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
    }
}
