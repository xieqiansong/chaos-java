package lan.chaos.simple.web;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MiniHttpServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("HTTP Server started on port 8080");

        while (true) {
            Socket socket = serverSocket.accept(); // 阻塞等待请求
            handleRequest(socket);
        }
    }

    private static void handleRequest(Socket socket) {
        try (
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream()
        ) {
            // 1. 读取请求行
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Request: " + requestLine);

            // 2. 简单解析（忽略 headers）
            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];

            // 3. 构造 HTTP 响应
            String body = "<h1>Hello from Mini HTTP Server</h1>";
            String response =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + body.getBytes().length + "\r\n" +
                "\r\n" +
                body;

            // 4. 发送响应
            out.write(response.getBytes());
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}