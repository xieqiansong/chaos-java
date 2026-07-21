package lan.chaos.jdk21features.recordpattern;

/**
 * Record 模式（JEP 440，JDK21 定稿）：在 instanceof / switch 中直接"解构" record，取出其组件。
 *
 * <p>WHY：过去拿到一个 {@code Line} 对象要逐个 {@code line.start().x()} 取值；Record 模式允许
 * {@code if (o instanceof Line(Point p1, Point p2))} 或 {@code case Line(Point p1, Point p2)} 一步解构，
 * 且支持嵌套（record 里套 record）。与密封类/模式匹配 switch 组合，能写出既安全又简洁的领域逻辑。
 */
public class RecordPatternDemo {

    public record Point(int x, int y) {
    }

    public record Line(Point start, Point end) {
    }

    public static void run() {
        Object o = new Line(new Point(0, 0), new Point(3, 4));

        // instanceof 中解构
        if (o instanceof Line(Point p1, Point p2)) {
            System.out.println("line start=(" + p1.x() + "," + p1.y() + ") end=(" + p2.x() + "," + p2.y() + ")");
        }

        // switch 中嵌套解构
        System.out.println("describe(line) = " + describe(new Line(new Point(0, 0), new Point(3, 4))));
        System.out.println("describe(point) = " + describe(new Point(1, 2)));
    }

    static String describe(Object o) {
        return switch (o) {
            case Line(Point p1, Point p2) ->
                    "线段长=" + Math.hypot(p2.x() - p1.x(), p2.y() - p1.y());
            case Point(int x, int y) -> "点(" + x + "," + y + ")";
            default -> "?";
        };
    }
}
