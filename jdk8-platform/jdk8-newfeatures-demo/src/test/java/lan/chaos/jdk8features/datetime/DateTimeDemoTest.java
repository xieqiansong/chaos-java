package lan.chaos.jdk8features.datetime;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeDemoTest {

    @Test
    void immutablePlusAndParse() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        assertTrue(nextWeek.isAfter(today));
        assertEquals(today.plusDays(7), nextWeek); // 运算返回新对象，原对象不变

        LocalDate parsed = LocalDate.parse("2024-01-15");
        assertEquals(2024, parsed.getYear());
        assertEquals(1, parsed.getMonthValue());
    }
}
