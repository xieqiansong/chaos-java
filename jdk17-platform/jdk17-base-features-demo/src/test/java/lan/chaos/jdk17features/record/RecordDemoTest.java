package lan.chaos.jdk17features.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordDemoTest {

    @Test
    void recordAccessorsAndMethods() {
        RecordDemo.Point p = new RecordDemo.Point(3, 4);
        assertEquals(3, p.x());
        assertEquals(4, p.y());
        assertEquals(5.0, p.distanceFromOrigin(), 1e-9);
    }

    @Test
    void compactConstructorValidation() {
        RecordDemo.Range valid = new RecordDemo.Range(1, 10);
        assertEquals(1, valid.lo());
        assertEquals(10, valid.hi());

        assertThrows(IllegalArgumentException.class, () -> new RecordDemo.Range(10, 1));
    }
}
