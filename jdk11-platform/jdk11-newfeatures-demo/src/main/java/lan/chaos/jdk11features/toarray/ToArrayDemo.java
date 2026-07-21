package lan.chaos.jdk11features.toarray;

import java.util.Arrays;
import java.util.List;

/**
 * Collection.toArray(IntFunction)（JDK11）：一行得到正确类型的数组，替代 {@code toArray(new String[0])}。
 *
 * <p>WHY：旧写法 {@code list.toArray(new String[0])} 需要传入一个"仅供类型推断"的空数组，别扭；
 * {@code list.toArray(String[]::new)} 直接用构造器引用，类型安全且清晰。
 */
public class ToArrayDemo {

    public static void run() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] arr = list.toArray(String[]::new);
        System.out.println("toArray(String[]::new): " + Arrays.toString(arr));
        System.out.println("length: " + arr.length + ", [0]: " + arr[0]);
    }
}
