package lan.chaos.java.io;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@Slf4j
public class FileCopyDemo {

    /**
     * 传统字节流（基础版 - 逐字节拷贝）
     */
    public static void copyByByteStream(String src, String dest) throws IOException {
        @Cleanup
        InputStream in = Files.newInputStream(Paths.get(src));
        @Cleanup
        OutputStream out = Files.newOutputStream(Paths.get(dest));
        int byteRead;
        while ((byteRead = in.read()) != -1) {
            out.write(byteRead);
        }
    }


    /**
     * 字符流版（仅限文本文件）
     */
    public static void copyTextByReaderWriter(String src, String dest) throws IOException {
        @Cleanup
        BufferedReader reader = new BufferedReader(new FileReader(src));
        @Cleanup
        BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }
    }


    /**
     * 带缓冲区的字节流（最常用）
     */
    public static void copyByBufferedStream(String src, String dest) throws IOException {
        @Cleanup
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
        @Cleanup
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        // 8KB 缓冲区
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }

    /**
     * NIO FileChannel 直接复制
     */
    public static void copyByNIOChannel(String src, String dest) throws Exception {
        @Cleanup
        FileInputStream is = new FileInputStream(src);
        @Cleanup
        FileOutputStream os = new FileOutputStream(dest);
        FileChannel inChannel = is.getChannel();
        FileChannel outChannel = os.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
    }

    /**
     * NIO FileChannel 直接复制2
     */
    public static void copyFile6(String src, String dist) throws IOException {
        @Cleanup
        FileInputStream is = new FileInputStream(src);
        @Cleanup
        FileOutputStream os = new FileOutputStream(dist);
        FileChannel inChannel = is.getChannel();
        FileChannel fcout = os.getChannel();
        /* 为缓冲区分配 8192 个字节 */
        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
        while (true) {
            /* 从输入通道中读取数据到缓冲区中 */
            int r = inChannel.read(buffer);
            /* read() 返回 -1 表示 EOF */
            if (r == -1) {
                break;
            }
            /* 切换读写 */
            buffer.flip();
            /* 把缓冲区的内容写入输出文件中 */
            fcout.write(buffer);
            /* 清空缓冲区 */
            buffer.clear();
        }
    }

    /**
     * Java 7+ Files.copy（最简洁）
     */
    public static void copyByFilesClass(String src, String dest) throws Exception {
        Path source = Paths.get(src);
        Path target = Paths.get(dest);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }


    /**
     * 内存映射文件（超大文件优化）
     */
    public static void copyByMappedMemory(String src, String dest) throws Exception {
        @Cleanup
        RandomAccessFile inFile = new RandomAccessFile(src, "r");
        @Cleanup
        RandomAccessFile outFile = new RandomAccessFile(dest, "rw");
        FileChannel inChannel = inFile.getChannel();
        FileChannel outChannel = outFile.getChannel();

        MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        int ignored = outChannel.write(buffer);
    }

    /**
     * Java 9+ 流式复制（高效传输）
     */
//    public static void copyJava9Stream(String src, String dest) throws IOException {
//        @Cleanup
//        InputStream in = new FileInputStream(src);
//        @Cleanup
//        OutputStream out = new FileOutputStream(dest);
//        // Java 9 新增方法
//        in.transferTo(out);
//    }
    public static void main(String[] args) {
        String src = "D:/data/temp/big_file.txt";
        String dest = "D:/data/temp/big_file_copy.txt";
        Method[] methods = FileCopyDemo.class.getDeclaredMethods();
        Arrays.stream(methods).filter(m -> m.getName().startsWith("copy")).forEach(method -> {
            try {
                FileUtil.del(new File(dest));
                long start = System.currentTimeMillis();
                method.invoke(null, src, dest);
                Console.log("{} {}ms", method.getName(), System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }


}
