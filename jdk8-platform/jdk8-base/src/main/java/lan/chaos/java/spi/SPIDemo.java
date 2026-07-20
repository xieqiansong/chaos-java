package lan.chaos.java.spi;

import java.util.List;
import java.util.ServiceLoader;

/**
 * SPI（Service Provider Interface），是JDK内置的一种 服务提供发现机制，可以用来启用框架扩展和替换组件，主要是被框架的开发人员使用。
 */
public class SPIDemo {

    public static void main(String[] args) {
        ServiceLoader<Search> searchImpls = ServiceLoader.load(Search.class);
        for (Search search : searchImpls) {
            List<String> ignored = search.searchDoc("hello world");
        }
    }
}
