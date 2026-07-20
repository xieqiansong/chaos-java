package lan.chaos.simple.web;

import java.io.IOException;

public interface Servlet {
    void service(ServletRequest request, ServletResponse response) throws ServletException, IOException;
}
