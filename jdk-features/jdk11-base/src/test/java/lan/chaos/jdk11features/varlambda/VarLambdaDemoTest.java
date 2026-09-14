package lan.chaos.jdk11features.varlambda;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VarLambdaDemoTest {

    @Test
    void varInLambda() {
        Predicate<String> isEmpty = (var s) -> s.isEmpty();
        assertTrue(isEmpty.test(""));

        Function<String, Integer> len = (var s) -> s.length();
        assertEquals(5, len.apply("hello"));
    }
}
