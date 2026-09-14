package lan.chaos.jdk17features.patterninstanceof;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternInstanceOfDemoTest {

    @Test
    void patternMatching() {
        Object o = "hello";
        assertTrue(o instanceof String s && s.length() == 5);
        assertTrue(o instanceof String s && s.length() > 3);

        Object n = 42;
        assertFalse(n instanceof String s);
    }
}
