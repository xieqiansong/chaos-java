package lan.chaos.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileAppender;
import cn.hutool.core.util.StrUtil;
import lombok.Cleanup;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WordFrequencyCounter {

    private static final Set<String> TEXT_EXTENSIONS = Sets.newHashSet("java", "txt", "yaml", "yml", "md", "properties");

    private static final Set<String> cocaSet = new HashSet<>();


    public static void count(String dir, String outFile) throws Exception {
        cocaSet.addAll(FileUtil.readLines("D:/project/confusion/workspace/data/coca20000.txt", StandardCharsets.UTF_8));

        Path directory = Paths.get(dir);
        if (!Files.isDirectory(directory)) {
            System.out.println("Invalid directory path: " + dir);
            return;
        }

        List<Path> files = collectTextFiles(directory);
        Map<String, Long> wordFrequency = countWordFrequency(files);

        FileAppender appender = new FileAppender(new File(outFile), 16, true);


        // 输出前50个高频单词
        wordFrequency.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .filter(entry -> entry.getKey().length() > 1)
                .limit(5000)
                .forEach(entry -> {
                    appender.append(entry.getKey());
                });
        appender.flush();

    }

    // 收集所有文本文件路径
    private static List<Path> collectTextFiles(Path directory) throws IOException {
        IOFileFilter filter = new SuffixFileFilter(
                TEXT_EXTENSIONS.stream().map(s -> "." + s).collect(Collectors.toList())
        );
        @Cleanup
        Stream<Path> paths = Files.walk(directory);
        return paths
                .filter(Files::isRegularFile)
                .filter(path -> filter.accept(path.toFile()))
                .collect(Collectors.toList());

    }

    // 多线程统计词频
    private static Map<String, Long> countWordFrequency(List<Path> files)
            throws InterruptedException, ExecutionException {

        ExecutorService executor = ForkJoinPool.commonPool();
        List<Future<Map<String, Long>>> futures = new ArrayList<>();

        for (Path file : files) {
            futures.add(executor.submit(() -> processFile(file)));
        }

        // 使用Guava的并发哈希集进行合并
        ConcurrentMap<String, Long> resultMap = new ConcurrentHashMap<>();
        for (Future<Map<String, Long>> future : futures) {
            Map<String, Long> partialMap = future.get();
            partialMap.forEach((word, count) ->
                    resultMap.merge(word.toLowerCase(), count, Long::sum)
            );
        }
        executor.shutdown();
        return resultMap;

    }

/*
    // 处理单个文件（使用文件流防止内存溢出）
    private static Map<String, Long> processFile(Path file) {
        Map<String, Long> wordCount = new HashMap<>();
        try {
            @Cleanup
            BufferedReader reader = Files.newBufferedReader(file);
            String line;
            while ((line = reader.readLine()) != null) {
                // 使用正则表达式分割单词
                String[] words = line.split("[\\s\\p{Punct}]+");
                for (String word : words) {
                    if (!word.isBlank()) {
                        wordCount.merge(word.toLowerCase(), 1L, Long::sum);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + file + " - " + e.getMessage());
        }
        return wordCount;
    }
*/

    // 处理单个文件（使用文件流防止内存溢出）
    private static Map<String, Long> processFile(Path file) {
        Map<String, Long> wordCount = new HashMap<>();
        try {
            @Cleanup
            BufferedReader reader = Files.newBufferedReader(file);
            String line;
            while ((line = reader.readLine()) != null) {
                // 修改正则表达式：只匹配连续的英文字母作为单词
                String[] words = line.split("[^a-zA-Z]+");

                for (String word : words) {
                    if (StrUtil.isNotBlank(word) && cocaSet.contains(word)) {
                        wordCount.merge(word.toLowerCase(), 1L, Long::sum);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + file + " - " + e.getMessage());
        }
        return wordCount;
    }

}
