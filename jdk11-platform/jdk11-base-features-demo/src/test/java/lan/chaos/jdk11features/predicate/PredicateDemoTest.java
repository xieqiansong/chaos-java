package lan.chaos.jdk11features.predicate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredicateDemoTest {

    @Test
    void notAndNonNull() {
        List<String> list = Arrays.asList("a", "", "b", null, "c");

        List<String> nonEmpty = list.stream()
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b", "c"), nonEmpty);
    }
}
