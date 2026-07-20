package lan.chaos.simple.web;

import java.io.OutputStream;
import java.io.PrintWriter;

public class Response implements ServletResponse {

    private static final int BUFFER_SIZE = 1024;
    Request request;
    OutputStream output;

    public Response(OutputStream output) {
        this.output = output;
    }

    // response中封装了request，以便获取request中的请求参数
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(output);
    }
}