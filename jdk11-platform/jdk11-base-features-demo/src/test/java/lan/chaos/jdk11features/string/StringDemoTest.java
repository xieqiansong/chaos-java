package lan.chaos.jdk11features.string;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringDemoTest {

    @Test
    void newStringMethods() {
        assertTrue("".isBlank());
        assertTrue("   ".isBlank());
        assertEquals("hi", "  hi  ".strip());
        assertEquals("ababab", "ab".repeat(3));
        assertEquals(3, "line1\nline2\nline3".lines().count());
    }
}
