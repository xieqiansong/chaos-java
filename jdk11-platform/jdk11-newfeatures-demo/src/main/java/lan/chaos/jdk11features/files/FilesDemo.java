package lan.chaos.jdk11features.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Files.readString / writeString（JDK11）：一行完成文件读写，替代繁琐的 Files.readAllBytes + 解码。
 *
 * <p>WHY：旧写法要 {@code new String(Files.readAllBytes(path), charset)}，繁琐且易漏字符集。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code Files.writeString(path, content)} / {@code Files.readString(path)} 默认 UTF-8；</li>
 *   <li>{@code Path.of(...)} 替代 {@code Paths.get(...)}，更简洁；</li>
 *   <li>读写失败抛 {@code IOException}，生产应妥善处理。</li>
 * </ul>
 */
public class FilesDemo {

    public static void run() {
        try {
            Path path = Files.createTempFile("jdk11-", ".txt");
            Files.writeString(path, "JDK11 Files.writeString \u6587\u672c");
            String content = Files.readString(path);
            System.out.println("写入并读回: " + content);
            System.out.println("equals 原内容: " + content.equals("JDK11 Files.writeString \u6587\u672c"));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
