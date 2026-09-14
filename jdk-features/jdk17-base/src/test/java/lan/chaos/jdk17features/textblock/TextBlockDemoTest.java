package lan.chaos.jdk17features.textblock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextBlockDemoTest {

    @Test
    void textBlocksAndHelpers() {
        String tb = """
                line1
                line2""";
        assertTrue(tb.contains("line1"));
        assertTrue(tb.contains("line2"));

        assertEquals("Hello 18", "Hello %d".formatted(18));
        assertEquals("a\nb\tc", "a\\nb\\tc".translateEscapes());
    }
}
