package lan.chaos.jdk17features.switchexpression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwitchExpressionDemoTest {

    @Test
    void switchExpressionReturnsValue() {
        SwitchExpressionDemo.Day sat = SwitchExpressionDemo.Day.SAT;
        String level = switch (sat) {
            case SAT, SUN -> "weekend";
            default -> "weekday";
        };
        assertEquals("weekend", level);

        SwitchExpressionDemo.Day mon = SwitchExpressionDemo.Day.MON;
        int hours = switch (mon) {
            case MON, TUE, WED, THU, FRI -> 8;
            case SAT -> 4;
            case SUN -> 0;
        };
        assertEquals(8, hours);
    }
}
