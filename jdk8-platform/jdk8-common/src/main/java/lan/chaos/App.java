package lan.chaos;

import cn.hutool.core.codec.Base62;
import cn.hutool.core.codec.Base62Codec;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hello world!
 *
 */
abstract public class App {
    public static void main(String[] args) {
        HashMap<String, String> map = new HashMap<>();
        map.put("1", "1");

        ConcurrentHashMap<String, Object> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("1", "1");
        concurrentHashMap.get("1");

        ByteBuffer buffer = ByteBuffer.allocate(4);

        int x = 5;
        Object obj = (x>4) ? 99.9:"a";

        System.out.println(Math.abs(Integer.MIN_VALUE));
        System.out.println(Math.abs(Long.MIN_VALUE));

    }

    public static void helloWorld() {
        System.out.println("Hello World!");
    }
}
