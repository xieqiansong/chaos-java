package lan.chaos.simple.web;

import cn.hutool.core.io.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticResourceProcessor {
    public void process(Request request, Response response) {
        OutputStream output = response.output;

        // 读取文件内容
        try {
            File file = new File(ServletConfig.WEB_ROOT, request.getUri());
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    // 添加HTTP响应头
                    String contentType = Files.probeContentType(file.toPath());
                    output.write(("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + contentType + "\r\n" +
                            "Content-Length: " + file.length() + "\r\n\r\n").getBytes());
                    IoUtil.copy(fis, output);
                }
            } else {
                // 文件不存在时，输出404信息
                String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 23\r\n" +
                        "\r\n" +
                        "<h1>File Not Found</h1>";
                output.write(errorMessage.getBytes());
            }
        } catch (Exception e) {
            // thrown if cannot instantiate a File object
            System.out.println(e.toString());
        }
    }
}
