package lan.chaos.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 空文件夹清理工具。
 *
 * <p>入参：
 * <ul>
 *     <li>args[0] 必填：待清理的根目录路径</li>
 *     <li>args[1] 可选：模式标记，
 *         {@code -p} / {@code --print} / {@code --dry-run} 表示只打印待删除的空文件夹而不真正删除；
 *         省略则默认执行实际删除</li>
 * </ul>
 *
 * <p>采用后序（自底向上）遍历：先递归处理子目录，再判断当前目录是否为空，
 * 从而能连片删除因子目录被清空而变空的父目录。
 */
public class EmptyDirCleaner {

    /** 是否只打印不删除 */
    private static boolean dryRun = false;

    /** 待删除 / 已删除的空文件夹计数 */
    private static final AtomicInteger deletedCount = new AtomicInteger(0);
    private static final AtomicInteger failedCount = new AtomicInteger(0);

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String rootPath = args[0];
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("-p".equals(arg) || "--print".equals(arg) || "--dry-run".equals(arg)) {
                dryRun = true;
            } else {
                System.err.println("未知参数，已忽略: " + arg);
            }
        }

        Path root = Paths.get(rootPath);
        if (!Files.exists(root)) {
            System.err.println("路径不存在: " + rootPath);
            return;
        }
        if (!Files.isDirectory(root)) {
            System.err.println("不是目录: " + rootPath);
            return;
        }

        System.out.println((dryRun ? "[仅打印模式] " : "[删除模式] ")
                + "开始扫描目录: " + root.toAbsolutePath());

        try {
            clean(root);
        } catch (IOException e) {
            System.err.println("扫描过程中发生异常: " + e.getMessage());
        }

        System.out.println("------------------------------");
        System.out.println("扫描完成。空文件夹数量: " + deletedCount.get()
                + (dryRun ? " (仅打印)" : " (已删除)"));
        if (failedCount.get() > 0) {
            System.out.println("删除失败数量: " + failedCount.get());
        }
    }

    /**
     * 递归清理空文件夹（后序遍历）。
     *
     * @param dir 当前目录
     * @return 当前目录是否为空（若为空且父目录需要知道）
     * @throws IOException 列出目录失败时抛出
     */
    private static boolean clean(Path dir) throws IOException {
        List<Path> children = listChildren(dir);

        // 1. 先处理所有子目录（自底向上）
        boolean allChildrenRemoved = true;
        for (Path child : children) {
            if (Files.isDirectory(child)) {
                boolean childEmpty = clean(child);
                if (!childEmpty) {
                    allChildrenRemoved = false;
                }
            } else {
                // 存在文件，说明本目录不能直接算空
                allChildrenRemoved = false;
            }
        }

        // 重新列出，确认本目录是否真的为空
        List<Path> current = listChildren(dir);
        if (!current.isEmpty()) {
            return false;
        }

        // 2. 当前目录为空，按模式处理
        if (dryRun) {
            System.out.println("[待删除] " + dir.toAbsolutePath());
            deletedCount.incrementAndGet();
        } else {
            try {
                Files.delete(dir);
                System.out.println("[已删除] " + dir.toAbsolutePath());
                deletedCount.incrementAndGet();
            } catch (IOException e) {
                failedCount.incrementAndGet();
                System.err.println("[删除失败] " + dir.toAbsolutePath() + " -> " + e.getMessage());
            }
        }
        return true;
    }

    /**
     * 列出目录子项；目录不可访问时返回空列表并记录。
     */
    private static List<Path> listChildren(Path dir) {
        try {
            List<Path> result = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                stream.forEach(result::add);
            }
            return result;
        } catch (NoSuchFileException e) {
            // 已被上层删除
            return Collections.emptyList();
        } catch (IOException e) {
            System.err.println("[无法访问目录] " + dir.toAbsolutePath() + " -> " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static void printUsage() {
        System.out.println("用法: EmptyDirCleaner <路径> [模式]");
        System.out.println("  参数:");
        System.out.println("    <路径>         必填，待清理的根目录");
        System.out.println("    -p / --print / --dry-run   可选，只打印待删除的空文件夹，不真正删除");
        System.out.println("  示例:");
        System.out.println("    java EmptyDirCleaner D:/tmp/empty-test          # 实际删除空文件夹");
        System.out.println("    java EmptyDirCleaner D:/tmp/empty-test --print  # 只打印不删除");
    }
}
