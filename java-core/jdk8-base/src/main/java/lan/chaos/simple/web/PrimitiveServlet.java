package lan.chaos.simple.web;

import java.io.IOException;
import java.io.PrintWriter;

public class PrimitiveServlet implements Servlet {

    public void init(ServletConfig config) throws ServletException {
        System.out.println("init");
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        // 添加HTTP响应头
        out.println("HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 35\r\n\r\n");

        out.println("Hello. Roses are red.");
        out.print("Violets are blue.");

        out.flush();
    }

    public void destroy() {
        System.out.println("destroy");
    }

    public String getServletInfo() {
        return null;
    }

    public ServletConfig getServletConfig() {
        return null;
    }

}