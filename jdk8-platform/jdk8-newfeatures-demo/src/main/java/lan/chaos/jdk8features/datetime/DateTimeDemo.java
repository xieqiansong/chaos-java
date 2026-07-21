package lan.chaos.jdk8features.datetime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 新的日期时间 API（JDK8，{@code java.time} 包，JSR-310）：解决旧 Date/Calendar 可变、时区混乱、线程不安全。
 *
 * <p>WHY：{@code java.util.Date} 年份从 1900 起算、月份 0 起算、可变且非线程安全；{@code SimpleDateFormat} 也非线程安全。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code LocalDate/LocalTime/LocalDateTime} 不可变、线程安全；</li>
 *   <li>{@code Instant} 表示时间线上的点（UTC）；{@code Duration} 时长、{@code Period} 日期区间；</li>
 *   <li>{@code DateTimeFormatter} 线程安全，替代 {@code SimpleDateFormat}；</li>
 *   <li>所有运算（plusDays 等）返回<b>新对象</b>，原对象不变。</li>
 * </ul>
 */
public class DateTimeDemo {

    public static void run() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        System.out.println("今天: " + today + "，下周: " + nextWeek);

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("格式化: " + now.format(fmt));

        // 解析
        LocalDate parsed = LocalDate.parse("2024-01-15");
        Period period = Period.between(parsed, today);
        System.out.println("距 2024-01-15: " + period.getYears() + "年" + period.getMonths() + "月" + period.getDays() + "天");

        // 时长
        Instant start = Instant.now();
        Instant after = start.plus(Duration.ofSeconds(30));
        System.out.println("30秒后(Instant)相差: " + Duration.between(start, after).getSeconds() + "s");

        // 带时区
        ZonedDateTime bj = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        ZonedDateTime ny = bj.withZoneSameInstant(ZoneId.of("America/New_York"));
        System.out.println("北京: " + bj.format(fmt) + " | 纽约: " + ny.format(fmt));
    }
}
