package lan.chaos.jdk17features.patterninstanceof;

/**
 * instanceof 模式匹配（JEP 394，JDK16 定稿）：{@code if (obj instanceof String s)} 一步完成"判断 + 类型转换 + 绑定变量"。
 *
 * <p>WHY：旧写法要先 {@code instanceof} 判断，再强制转型 {@code (String) obj}，重复且易错（两次判断易不一致）。
 * 关键规则：
 * <ul>
 *   <li>绑定变量 {@code s} 仅在判断为真的分支内可见；</li>
 *   <li>可与 {@code &&} 串联进一步收窄（如 {@code && s.length() > 3}），但 {@code ||} 后作用域不可见。</li>
 * </ul>
 */
public class PatternInstanceOfDemo {

    public static void run() {
        Object o = "hello";

        // 旧写法
        if (o instanceof String) {
            String s = (String) o;
            System.out.println("旧写法 length: " + s.length());
        }

        // 新写法：判断即转型
        if (o instanceof String s) {
            System.out.println("新模式 length: " + s.length());
        }

        // && 串联收窄
        if (o instanceof String s && s.length() > 3) {
            System.out.println("length>3 转大写: " + s.toUpperCase());
        }
    }
}
