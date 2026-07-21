package lan.chaos.jdk17features.record;

/**
 * Record 记录类（JEP 395，JDK16 定稿）：一种透明的不可变数据载体。
 *
 * <p>WHY：以前写 DTO / 值对象要手写 field、getter、equals、hashCode、toString、构造器，样板代码极多。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code record Point(int x, int y)} 自动生成私有 final 字段、全参构造器、访问器 {@code x()}/{@code y()}、equals/hashCode/toString；</li>
 *   <li>可在 record 体内加普通方法（如 {@code distanceFromOrigin}），也能覆盖默认访问器逻辑；</li>
 *   <li>紧凑构造器（compact constructor）只在 {@code (...)} 里写校验，参数名即字段名，自动赋值。</li>
 * </ul>
 * 生产坑点：Record 默认不可变（字段 final），适合值对象；若需要可变请用普通 class。Record 不能继承其他类（隐式 final extends Record）。
 */
public class RecordDemo {

    public static void run() {
        Point p = new Point(3, 4);
        System.out.println("Point: " + p);
        System.out.println("到原点距离: " + p.distanceFromOrigin());

        // 紧凑构造器校验：lo 必须 <= hi
        System.out.println("合法 Range: " + new Range(1, 10));
        try {
            new Range(10, 1);
        } catch (IllegalArgumentException e) {
            System.out.println("非法 Range 被拦截: " + e.getMessage());
        }
    }

    /** 带普通方法的 Record：计算到原点距离。 */
    public record Point(int x, int y) {
        public double distanceFromOrigin() {
            return Math.sqrt(x * x + y * y);
        }
    }

    /** 带紧凑构造器校验的 Record。 */
    public record Range(int lo, int hi) {
        public Range {
            if (lo > hi) {
                throw new IllegalArgumentException("lo(" + lo + ") 不能大于 hi(" + hi + ")");
            }
        }
    }
}
