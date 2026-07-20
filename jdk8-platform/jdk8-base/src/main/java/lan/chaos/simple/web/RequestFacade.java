package lan.chaos.simple.web;

import java.util.Enumeration;

public class RequestFacade implements ServletRequest {

    private ServletRequest request = null;

    public RequestFacade(Request request) {
        this.request = request;
    }

    /* implementation of the ServletRequest*/
    public Object getAttribute(String attribute) {
        return request.getAttribute(attribute);
    }

    public Enumeration getAttributeNames() {
        return request.getAttributeNames();
    }

    public String getRealPath(String path) {
        return request.getRealPath(path);
    }
}