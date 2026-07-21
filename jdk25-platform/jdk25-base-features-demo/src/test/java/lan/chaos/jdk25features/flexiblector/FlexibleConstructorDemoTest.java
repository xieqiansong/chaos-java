package lan.chaos.jdk25features.flexiblector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlexibleConstructorDemoTest {

    @Test
    void validPositive() {
        FlexibleConstructorDemo.Positive p = new FlexibleConstructorDemo.Positive(5);
        assertEquals(25, p.squared());
    }

    @Test
    void rejectsNegativeBeforeSuper() {
        assertThrows(IllegalArgumentException.class, () -> new FlexibleConstructorDemo.Positive(-1));
    }
}
