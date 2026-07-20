package lan.chaos.simple.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {


    // 关闭Server的请求
    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    // 是否关闭Server
    private boolean shutdown = false;

    // 主入口
    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.await();
    }

    public void await() {
        // 启动ServerSocket
        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // 循环等待一个Request请求
        while (!shutdown) {
            Socket socket = null;
            InputStream input = null;
            OutputStream output = null;
            try {
                // 创建socket
                socket = serverSocket.accept();
                input = socket.getInputStream();
                output = socket.getOutputStream();

                // 封装input至request, 并处理请求
                Request request = new Request(input);
                request.parse();

                // 封装output至response
                Response response = new Response(output);
                response.setRequest(request);

                // 而是如果以/servlet/开头，则委托ServletProcessor处理
                if (request.getUri().startsWith("/servlet/")) {
                    ServletProcessor1 processor = new ServletProcessor1();
                    processor.process(request, response);
                } else {
                    // 原有的静态资源处理
                    StaticResourceProcessor processor = new StaticResourceProcessor();
                    processor.process(request, response);
                }

                // 关闭socket
                socket.close();

                // 如果接受的是关闭请求，则设置关闭监听request的标志
                shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }
}