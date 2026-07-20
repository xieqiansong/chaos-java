package lan.chaos;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hello world!
 */
public class JavaTest {
    private static final int MAX_CAPACITY = 10;

    public static void main(String[] args) {
        TreeMap<String, String> treeMap = new TreeMap<>();
        HashMap<String, String> hashMap = new HashMap<>();
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_CAPACITY;
            }
        };
    }
}
