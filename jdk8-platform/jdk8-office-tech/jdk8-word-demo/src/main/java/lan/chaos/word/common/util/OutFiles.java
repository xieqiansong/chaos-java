package lan.chaos.word.common.util;

import lan.chaos.word.common.config.WordProperties;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 产物落盘：所有场景生成的 docx/doc 统一写到配置目录（默认 target/out）。
 *
 * <p>WHY：产物必须落在被 .gitignore 忽略的 target/ 下——
 * ① 二进制文件进 git 会污染仓库且无法 diff；
 * ② 但文件又必须真实落盘，因为「能用 Word / WPS 打开」是这类 Demo 唯一的强验证。
 */
@Component
public class OutFiles {

    private final String outDir;

    public OutFiles(WordProperties properties) {
        this.outDir = properties.getOutDir();
    }

    /** 返回输出目录下的文件（自动建目录）。 */
    public File of(String name) {
        File dir = new File(outDir);
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
            throw new IllegalStateException("无法创建输出目录：" + dir.getAbsolutePath());
        }
        return new File(dir, name);
    }

    /** 人类可读的大小（KB/MB）。 */
    public static String readableSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
