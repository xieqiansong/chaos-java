package lan.chaos.jdk11features.files;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesDemoTest {

    @Test
    void writeAndReadString() throws Exception {
        Path path = Files.createTempFile("jdk11-test-", ".txt");
        try {
            Files.writeString(path, "JDK11 \u8bfb\u5199");
            assertEquals("JDK11 \u8bfb\u5199", Files.readString(path));
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
