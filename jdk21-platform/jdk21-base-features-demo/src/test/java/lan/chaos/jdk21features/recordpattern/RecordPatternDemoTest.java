package lan.chaos.jdk21features.recordpattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordPatternDemoTest {

    @Test
    void deconstructInInstanceof() {
        RecordPatternDemo.Line line = new RecordPatternDemo.Line(
                new RecordPatternDemo.Point(0, 0), new RecordPatternDemo.Point(3, 4));
        Object o = line;
        assertTrue(o instanceof RecordPatternDemo.Line(RecordPatternDemo.Point p1, RecordPatternDemo.Point p2)
                && p1.x() == 0 && p2.x() == 3);
    }

    @Test
    void deconstructInSwitch() {
        RecordPatternDemo.Line line = new RecordPatternDemo.Line(
                new RecordPatternDemo.Point(0, 0), new RecordPatternDemo.Point(3, 4));
        assertEquals("线段长=5.0", RecordPatternDemo.describe(line));
        assertEquals("点(1,2)", RecordPatternDemo.describe(new RecordPatternDemo.Point(1, 2)));
    }
}
