package lan.chaos.java.base.generic;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 能力四：泛型类型擦除 —— 理解 Java 泛型在编译期的"假泛型"机制。
 *
 * <p>WHY：Java 的泛型是编译期行为（伪泛型），运行时类型信息会被擦除为原始类型（Object 或上界）。
 * 这导致四个常见陷阱：
 * <ol>
 *   <li>List&lt;String&gt; 和 List&lt;Integer&gt; 的 Class 对象相同（都是 List.class）</li>
 *   <li>无法用 instanceof 检查泛型参数（只能 instance of List，不能 instance of List&lt;String&gt;）</li>
 *   <li>无法创建泛型数组（new T[] 编译错误，new List&lt;String&gt;[10] 编译警告）</li>
 *   <li>方法重载签名只看原始类型（擦除后 List&lt;String&gt; 和 List&lt;Integer&gt; 参数签名相同）</li>
 * </ol>
 *
 * <p>关键技巧：
 * <ul>
 *   <li>子类继承时保留父类泛型信息（通过 getGenericSuperclass() 获取 ParameterizedType）</li>
 *   <li>Gson/Jackson 的 TypeToken 原理即是利用匿名子类捕获泛型参数</li>
 *   <li>边界通配符：? extends T（生产者，只能读） vs ? super T（消费者，只能写）—— PECS 原则</li>
 * </ul>
 *
 * @see GenericTypeTokenDemo
 */
public class GenericTypeErasureDemo {

    /**
     * 陷阱1：List&lt;String&gt; 和 List&lt;Integer&gt; 的 Class 对象完全相同。
     * 因为泛型信息在编译后就被擦除了，运行时两者都是 ArrayList。
     */
    public String trapSameClass() {
        List<String> stringList = new ArrayList<>();
        List<Integer> intList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        sb.append("=== 陷阱1: 泛型 Class 对象相同 ===\n");
        sb.append("List<String>.getClass()  = ").append(stringList.getClass().getName()).append('\n');
        sb.append("List<Integer>.getClass() = ").append(intList.getClass().getName()).append('\n');
        sb.append("两者 Class 相同? ").append(stringList.getClass() == intList.getClass())
                .append("  ← 类型已擦除，运行时无法区分\n");

        // 可以通过反射绕过泛型检查
        List<String> guarded = new ArrayList<>();
        guarded.add("hello");
        try {
            // 编译期：guarded.add(123) 报错；运行时：反射绕过
            @SuppressWarnings("unchecked")
            List raw = guarded; // 原始类型绕过编译检查
            raw.add(123);
            sb.append("通过原始类型绕过编译检查：guarded.get(1) = \"").append(guarded.get(1)).append("\"\n");
        } catch (ClassCastException e) {
            sb.append("运行时 CCE: ").append(e.getMessage()).append('\n');
        }
        return sb.toString();
    }

    /**
     * 陷阱2：instanceof 无法检查泛型参数类型。
     */
    public String trapInstanceof() {
        List<String> list = new ArrayList<>();
        list.add("test");
        // 编译错误: illegal generic type for instanceof
        // if (list instanceof List<String>) { ... }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 陷阱2: instanceof 只能用原始类型 ===\n");
        sb.append("list instanceof List  → ").append(list instanceof List).append('\n');
        sb.append("if (list instanceof List<String>) → 编译错误: illegal generic type for instanceof\n");
        sb.append("解决办法：用 Class 对象做类型判断，如 list.get(0) instanceof String\n");
        return sb.toString();
    }

    /**
     * 陷阱3：泛型数组创建受限。
     * new T[10] 编译错误；new List&lt;String&gt;[10] 编译警告（unchecked）。
     */
    @SuppressWarnings("unchecked")
    public String trapGenericArray() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 陷阱3: 泛型数组 ===\n");
        sb.append("// T[] arr = new T[10];         ← 编译错误\n");
        sb.append("// List<String>[] arr = new List<String>[10]; ← 编译警告(unchecked)\n");

        // 变通方案1：用非泛型数组 + 强制转型
        List<String>[] arr = (List<String>[]) new ArrayList[3];
        arr[0] = new ArrayList<>();
        arr[0].add("hello");
        sb.append("变通: (List<String>[]) new ArrayList[3] → arr[0].get(0) = \"")
                .append(arr[0].get(0)).append("\"\n");

        // 变通方案2：直接 List<List<String>>
        List<List<String>> safe = new ArrayList<>();
        safe.add(new ArrayList<>());
        safe.get(0).add("better");
        sb.append("推荐: new ArrayList<List<String>>() → safe.get(0).get(0) = \"")
                .append(safe.get(0).get(0)).append("\"\n");

        return sb.toString();
    }

    /**
     * 技巧：通过匿名子类保留泛型信息（TypeToken 原理）。
     * Gson 的 new TypeToken&lt;List&lt;String&gt;&gt;(){} 就是这么工作的。
     */
    public String typeTokenPrinciple() {
        // 匿名子类会保留父类的泛型参数信息
        ArrayList<String> anonymous = new ArrayList<String>() {};

        Type superClass = anonymous.getClass().getGenericSuperclass();
        StringBuilder sb = new StringBuilder();
        sb.append("=== TypeToken 原理：匿名子类保留泛型信息 ===\n");
        sb.append("new ArrayList<String>(){} 的泛型父类 = ").append(superClass).append('\n');

        if (superClass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) superClass;
            Type[] actualTypes = pt.getActualTypeArguments();
            sb.append("actualTypeArguments[0] = ").append(actualTypes[0]).append('\n');
        }

        // 对比：非匿名子类获取不到
        ArrayList<String> plain = new ArrayList<>();
        Type plainSuper = plain.getClass().getGenericSuperclass();
        sb.append("\nnew ArrayList<>() 的泛型父类 = ").append(plainSuper)
                .append("  ← 无法获取 String\n");
        return sb.toString();
    }

    /**
     * PECS 原则演示：Producer Extends, Consumer Super。
     *
     * <p>? extends T：只能从中读取（生产者），因为不知道具体子类型，写入不安全
     * <p>? super T：只能向其中写入（消费者），因为读取只能得到 Object
     */
    public String pecsPrinciple() {
        List<Integer> ints = new ArrayList<>();
        ints.add(1);
        ints.add(2);

        // Producer extends: 可以读，不能写
        List<? extends Number> producer = ints; // Integer extends Number
        Number n = producer.get(0); // 可以读，返回 Number
        // producer.add(3); ← 编译错误：? extends Number 无法确定具体类型

        // Consumer super: 可以写，读只能到 Object
        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(3); // 可以写 Integer（Integer 是 Number 的子类）
        Object obj = consumer.get(0); // 读只能到 Object

        StringBuilder sb = new StringBuilder();
        sb.append("=== PECS 原则 ===\n");
        sb.append("? extends Number (Producer) → 只能 get(), 返回 Number = ").append(n).append('\n');
        sb.append("? super Integer (Consumer) → 只能 add(Integer), 但 get() 只能到 Object = ").append(obj).append('\n');
        sb.append("\n口诀：Producer Extends, Consumer Super (PECS)\n");
        return sb.toString();
    }

    /* ========== 统一入口 ========== */

    public static void main(String[] args) {
        GenericTypeErasureDemo demo = new GenericTypeErasureDemo();
        System.out.println(demo.trapSameClass());
        System.out.println(demo.trapInstanceof());
        System.out.println(demo.trapGenericArray());
        System.out.println(demo.typeTokenPrinciple());
        System.out.println(demo.pecsPrinciple());
    }
}
