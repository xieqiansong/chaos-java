package lan.chaos.java.io.zerocopy;

import cn.hutool.core.lang.Console;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

public class CommonTest {
    private final static String CONTENT = "Zero copy implemented by MappedByteBuffer";
    private final static String FILE_NAME = "D:\\tmp\\mmap.txt";
    private final static String CHARSET = "UTF-8";

    public static void writeToFileByMappedByteBuffer() {
        Path path = Paths.get(FILE_NAME);
        byte[] bytes = CONTENT.getBytes(Charset.forName(CHARSET));
        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(READ_WRITE, 0, bytes.length);
            if (mappedByteBuffer != null) {
                mappedByteBuffer.put(bytes);
                mappedByteBuffer.force();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readFromFileByMappedByteBuffer() {
        Path path = Paths.get(FILE_NAME);
        int length = CONTENT.getBytes(Charset.forName(CHARSET)).length;
        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(READ_ONLY, 0, length);
            if (mappedByteBuffer != null) {
                byte[] bytes = new byte[length];
                mappedByteBuffer.get(bytes);
                String content = new String(bytes, StandardCharsets.UTF_8);
                Console.log(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        writeToFileByMappedByteBuffer();
        readFromFileByMappedByteBuffer();
    }
}
