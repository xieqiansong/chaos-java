package lan.chaos.jdk25features.primitivepattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimitivePatternDemoTest {

    @Test
    void primitiveTypePatterns() {
        assertEquals("primitive int = 5", PrimitivePatternDemo.classify(Integer.valueOf(5)));
        assertEquals("primitive long = 7", PrimitivePatternDemo.classify(Long.valueOf(7L)));
        assertEquals("primitive double = 2.5", PrimitivePatternDemo.classify(Double.valueOf(2.5)));
        assertEquals("string len=4", PrimitivePatternDemo.classify("text"));
        assertTrue(PrimitivePatternDemo.classify(true).startsWith("other"));
    }
}
