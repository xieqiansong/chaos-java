package lan.chaos.simple.web;

import java.util.Enumeration;

public interface ServletRequest {
    Object getAttribute(String attribute);

    Enumeration getAttributeNames();

    String getRealPath(String path);
}
