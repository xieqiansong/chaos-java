package lan.chaos.jdk17features.sealed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SealedDemoTest {

    @Test
    void sealedHierarchy() {
        Shape circle = new Circle(2);
        assertTrue(circle instanceof Circle);
        assertEquals(Math.PI * 2 * 2, circle.area(), 1e-9);

        Shape rect = new Rectangle(3, 4);
        assertEquals(12.0, rect.area(), 1e-9);
    }

    @Test
    void exhaustivenessViaPatternMatching() {
        assertEquals("圆形 r=2.0", SealedDemo.describe(new Circle(2)));
        assertEquals("矩形 3.0x4.0", SealedDemo.describe(new Rectangle(3, 4)));
    }
}
