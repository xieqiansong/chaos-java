package lan.chaos.jdk8features.base64;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64（JDK8 纳入标准库 {@code java.util.Base64}，此前只能用 {@code sun.misc.BASE64Encoder} 等内部 API）。
 *
 * <p>WHY：编码/解码是高频需求，以前依赖 Sun 内部类（不同 JDK 实现可能缺失/变化），JDK8 起有了标准、稳定、无依赖的实现。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code getEncoder()/getDecoder()} 标准 Base64；</li>
 *   <li>{@code getUrlEncoder()} 把 {@code + /} 换成 {@code - _}，可用于 URL/文件名；</li>
 *   <li>{@code getMimeEncoder()} 用于邮件（每行 76 字符）。</li>
 * </ul>
 */
public class Base64Demo {

    public static void run() {
        String raw = "JDK8 新特性：Base64 标准化";
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        System.out.println("编码: " + encoded);

        byte[] decoded = Base64.getDecoder().decode(encoded);
        System.out.println("解码: " + new String(decoded, StandardCharsets.UTF_8));

        // URL 安全变体：不含 + 和 /
        String urlSafe = Base64.getUrlEncoder().encodeToString("a/b+c=".getBytes(StandardCharsets.UTF_8));
        System.out.println("URL安全编码: " + urlSafe);
    }
}
