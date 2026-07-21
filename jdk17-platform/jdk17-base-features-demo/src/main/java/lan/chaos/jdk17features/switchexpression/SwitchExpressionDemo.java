package lan.chaos.jdk17features.switchexpression;

/**
 * Switch 表达式（JEP 361，JDK14 定稿）：switch 可作为"表达式"返回值，引入 {@code ->} 与 {@code yield}。
 *
 * <p>WHY：旧 switch 语句靠 break 防穿透、靠变量在外层声明收集结果，易漏写 break 导致贯穿 bug。
 * 关键规则：
 * <ul>
 *   <li>{@code case LABEL -> 表达式} 不会贯穿，直接返回；</li>
 *   <li>多分支合并：{@code case MON, TUE, WED -> ...}；</li>
 *   <li>需要多语句时用 {@code case X -> { ...; yield 值; }}</li>
 * </ul>
 * 生产坑点：Switch 表达式必须"穷尽"（覆盖所有 case 或带 default / 枚举全列举），否则编译不过。
 */
public class SwitchExpressionDemo {

    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    public static void run() {
        Day d = Day.MON;

        // -> 语法，无贯穿
        int workHours = switch (d) {
            case MON, TUE, WED, THU, FRI -> 8;
            case SAT -> 4;
            case SUN -> 0;
        };
        System.out.println("MON 工作日: " + workHours + " 小时");

        // yield 语法（多语句块）
        String level = switch (d) {
            case SAT, SUN -> {
                System.out.println("周末分支");
                yield "weekend";
            }
            default -> {
                yield "weekday";
            }
        };
        System.out.println("MON 级别: " + level);
    }
}
