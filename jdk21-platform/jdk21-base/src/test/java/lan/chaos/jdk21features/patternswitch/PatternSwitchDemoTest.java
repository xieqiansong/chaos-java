package lan.chaos.jdk21features.patternswitch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternSwitchDemoTest {

    @Test
    void patternSwitchByType() {
        assertEquals("int 42", PatternSwitchDemo.describe(42));
        assertEquals("string len=5", PatternSwitchDemo.describe("hello"));
        assertEquals("double 3.14", PatternSwitchDemo.describe(3.14));
        assertEquals("bool true", PatternSwitchDemo.describe(true));
        assertEquals("other Object", PatternSwitchDemo.describe(new Object()));
    }
}
