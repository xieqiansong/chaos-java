package lan.chaos.pdf.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 在<b>运行期</b>读取依赖的真实版本——优先以「实际加载的 jar」为准。
 *
 * <p>WHY：PDF 专题最大的风险不是 API 用法，而是 <b>大版本错配</b>：
 * PDFBox 3.0 相对 2.0 是破坏性升级（{@code PDDocument.load} → {@code Loader.loadPDF}、
 * {@code PDType1Font.HELVETICA} 常量被移除），而 2.0.x 在 Maven 生态里仍被广泛传递引用。
 * 一旦 classpath 上混入 2.0.x，症状是运行期 NoSuchMethodError / NoClassDefFoundError，
 * 且堆栈落在 PDFBox 内部类里，极难定位。
 *
 * <p><b>两个探测源，且必须以 jar 为准：</b>
 * <ol>
 *   <li><b>实际加载的 jar 文件名</b>（最可靠）：类加载器告诉我们类来自哪个 jar，文件名就是真相。
 *       MANIFEST 的 Implementation-Version 作为次级来源。</li>
 *   <li><b>jar 内 META-INF/maven/.../pom.properties</b>（会失真）：
 *       实测 commons-compress-1.28.0.jar 里写的却是 1.23.0——Apache 部分构件的这份元数据并不可信，
 *       只能作为兜底。{@link #line} 会在两者不一致时把差异打印出来。</li>
 * </ol>
 */
public final class MavenVersion {

    private static final String[] MANIFEST_VERSION_KEYS =
            {"Implementation-Version", "Bundle-Version", "Specification-Version"};

    private MavenVersion() {
    }

    /** 策略1（兜底）：按 GAV 读 jar 内 pom.properties；读不到返回 absent。 */
    public static String of(String groupId, String artifactId) {
        String resource = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream in = MavenVersion.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return "absent";
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "unknown");
        } catch (IOException e) {
            return "error: " + e.getMessage();
        }
    }

    /** 策略2（权威）：probe 类实际加载自哪个 jar → 解析版本。 */
    public static String ofJar(Class<?> probe) {
        Path path = codeSourcePath(probe);
        if (path == null) {
            return "unknown(no codesource)";
        }
        if (Files.isDirectory(path)) {
            return "unknown(classes dir)";
        }
        String fromName = fromFileName(path);
        if (!fromName.startsWith("unknown")) {
            return fromName;
        }
        return manifestVersion(path);
    }

    /** 组合策略：以实际加载的 jar 为准，读不到再回退 pom.properties。 */
    public static String of(String groupId, String artifactId, Class<?> probe) {
        String jarVersion = ofJar(probe);
        return jarVersion.startsWith("unknown") ? of(groupId, artifactId) : jarVersion;
    }

    /**
     * 一行可读的 groupId:artifactId:version；当「jar 内 pom.properties」与「实际 jar」不一致时，
     * 一并打印出来——这种不一致是依赖排查中最容易踩空的坑。
     */
    public static String line(String groupId, String artifactId, Class<?> probe) {
        String actual = of(groupId, artifactId, probe);
        String declared = of(groupId, artifactId);
        if (!"absent".equals(declared) && !declared.equals(actual)) {
            return String.format("%s:%s:%s  (jar 内 pom.properties 写的是 %s，已按实际加载的 jar 为准)",
                    groupId, artifactId, actual, declared);
        }
        return groupId + ":" + artifactId + ":" + actual;
    }

    /** probe 类实际是从哪个 jar 加载的（文件名）。 */
    public static String jarOf(Class<?> probe) {
        Path path = codeSourcePath(probe);
        return path == null ? "unknown" : path.getFileName().toString();
    }

    private static Path codeSourcePath(Class<?> probe) {
        try {
            CodeSource codeSource = probe.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URL location = codeSource.getLocation();
            if (location == null || !"file".equals(location.getProtocol())) {
                return null;
            }
            return Paths.get(location.toURI());
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 jar 文件名解析版本（pdfbox-3.0.6.jar → 3.0.6）。 */
    private static String fromFileName(Path path) {
        String name = path.getFileName().toString();
        if (!name.endsWith(".jar")) {
            return "unknown(not a jar)";
        }
        String base = name.substring(0, name.length() - 4);
        int dash = base.lastIndexOf('-');
        if (dash < 0) {
            return "unknown(no version in filename)";
        }
        String version = base.substring(dash + 1);
        return Character.isDigit(version.charAt(0)) ? version : "unknown(no version in filename)";
    }

    private static String manifestVersion(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return "unknown(no manifest)";
            }
            Attributes attributes = manifest.getMainAttributes();
            for (String key : MANIFEST_VERSION_KEYS) {
                String v = attributes.getValue(key);
                if (v != null && !v.isEmpty()) {
                    return v;
                }
            }
            return "unknown(no version attr)";
        } catch (Exception e) {
            return "error: " + e;
        }
    }
}
