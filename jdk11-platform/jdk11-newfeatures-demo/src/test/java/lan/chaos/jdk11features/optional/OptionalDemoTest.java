package lan.chaos.jdk11features.optional;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalDemoTest {

    @Test
    void enhancements() {
        assertEquals("x", Optional.empty().or(() -> Optional.of("x")).get());
        assertEquals(1, Optional.of("a").stream().count());
        assertEquals(2, Stream.of(Optional.of("a"), Optional.empty(), Optional.of("b"))
                .flatMap(Optional::stream).count());
        assertTrue(Optional.empty().isEmpty());
    }
}
