package lan.chaos.simple.web;

import java.io.PrintWriter;

public class ResponseFacade implements ServletResponse {

    private Response response = null;

    public ResponseFacade(Response response) {
        this.response = response;
    }

    public PrintWriter getWriter() {
        return response.getWriter();
    }
}
