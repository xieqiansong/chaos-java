package lan.chaos.simple.web;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

public class ServletProcessor1 {

    public void process(Request request, Response response) {
        // 获取servlet名字
        String uri = request.getUri();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);

        // 初始化URLClassLoader
        URLClassLoader loader = null;
        try {
            // create a URLClassLoader
            URL[] urls = new URL[1];
            URLStreamHandler streamHandler = null;
            File classPath = new File(ServletConfig.WEB_ROOT);
            // the forming of repository is taken from the createClassLoader method in
            // org.apache.catalina.startup.ClassLoaderFactory
            String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString();
            // the code for forming the URL is taken from the addRepository method in
            // org.apache.catalina.loader.StandardClassLoader class.
            urls[0] = new URL(null, repository, streamHandler);
            loader = new URLClassLoader(urls);
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        // 用classLoader加载上面的servlet
        Class myClass = null;
        try {
            myClass = loader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }

        // 将加载到的class转成Servlet，并调用service方法处理


        Servlet servlet = null;
        RequestFacade requestFacade = new RequestFacade(request); // 转换成外观类
        ResponseFacade responseFacade = new ResponseFacade(response);// 转换成外观类
        try {
            servlet = (Servlet) myClass.newInstance();
            servlet.service((ServletRequest) requestFacade, (ServletResponse) responseFacade);
        } catch (Exception e) {
            System.out.println(e.toString());
        } catch (Throwable e) {
            System.out.println(e.toString());
        }

    }
}