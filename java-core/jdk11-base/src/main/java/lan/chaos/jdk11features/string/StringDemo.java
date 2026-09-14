package lan.chaos.jdk11features.string;

import java.util.List;
import java.util.stream.Collectors;

/**
 * String 新方法（JDK11）：补齐了空白处理、重复、按行拆分等长期缺失的 API。
 *
 * <p>WHY：以前去首尾空白只能用 {@code trim()}（只认 ASCII 空白、且连换行一起截）；重复字符串要手写循环。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code isBlank()} 认 Unicode 空白（含全角空格）；{@code strip()} 去首尾 Unicode 空白；</li>
 *   <li>{@code repeat(n)} 重复；{@code lines()} 按行拆成 Stream；{@code indent(n)} 整体缩进；</li>
 *   <li>{@code stripLeading()/stripTrailing()} 单侧去空白。</li>
 * </ul>
 */
public class StringDemo {

    public static void run() {
        System.out.println("isBlank(\"\"): " + "".isBlank());
        System.out.println("isBlank(\"  \"): " + "   ".isBlank());
        System.out.println("strip('  hi  '): '" + "  hi  ".strip() + "'");
        System.out.println("repeat('ab',3): " + "ab".repeat(3));

        String multi = "line1\nline2\nline3";
        List<String> lines = multi.lines().collect(Collectors.toList());
        System.out.println("lines count: " + lines.size() + " -> " + lines);

        // stripLeading/stripTrailing 单侧去空白
        System.out.println("stripLeading('  x  '): '" + "  x  ".stripLeading() + "'");
    }
}
