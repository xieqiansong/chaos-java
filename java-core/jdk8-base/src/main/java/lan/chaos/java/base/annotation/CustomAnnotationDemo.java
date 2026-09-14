package lan.chaos.java.base.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 能力五：自定义注解 + 运行时反射处理。
 *
 * <p>WHY：注解是 Java 元编程的基础设施，驱动力极强——Spring 的 @Autowired/@Transactional、
 * JUnit 的 @Test、Lombok 的 @Data 等都是注解驱动。
 * 理解自定义注解 + 运行时反射处理，才能看懂这些框架的底层原理。
 *
 * <p>关键概念：
 * <ul>
 *   <li>元注解：@Retention（SOURCE/CLASS/RUNTIME）、@Target（TYPE/FIELD/METHOD...）、@Inherited、@Documented</li>
 *   <li>注解元素：类型必须是 基本类型/String/Class/枚举/注解/以上的一维数组，不能是包装类型或自定义对象</li>
 *   <li>default 默认值：可不传；没有 default 的属性在使用时必须指定</li>
 *   <li>编译期处理（APT）vs 运行时反射：前者生成代码（Lombok/MapStruct），后者运行时读取（Spring/Jackson）</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>运行时注解通过反射读取，有性能开销，不适合高频调用的热点代码</li>
 *   <li>RUNTIME 注解会被保留到运行时，会增加方法区内存占用</li>
 *   <li>注解元素类型不能用包装类 Integer，必须用 int（或 Integer 的一维数组 Integer[]）</li>
 * </ul>
 *
 * @see GenericTypeErasureDemo
 */
public class CustomAnnotationDemo {

    // ====== 自定义注解定义 ======

    /**
     * 类级别注解：标记某个类为"审核实体"，审核通过后才能使用。
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
    public @interface Auditable {
        /** 审核人 */
        String reviewer();
        /** 审核级别，默认 1 */
        int level() default 1;
    }

    /**
     * 字段级别注解：标记字段为敏感信息，输出时需要脱敏。
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD})
    public @interface Sensitive {
        /** 脱敏类型 */
        SensitiveType type() default SensitiveType.MASK;
    }

    public enum SensitiveType {
        /** 全掩码（***） */
        MASK,
        /** 手机号脱敏（138****1234） */
        PHONE,
        /** 邮箱脱敏（ab***@domain.com） */
        EMAIL
    }

    // ====== 使用注解的示例类 ======

    @Auditable(reviewer = "admin", level = 2)
    static class User {
        private String name;

        @Sensitive(type = SensitiveType.PHONE)
        private String phone;

        @Sensitive(type = SensitiveType.EMAIL)
        private String email;

        public User(String name, String phone, String email) {
            this.name = name;
            this.phone = phone;
            this.email = email;
        }
    }

    // ====== 注解处理器 ======

    /**
     * 运行时读取 @Auditable 注解，检查是否需要审核。
     */
    public String processAuditable(Class<?> clazz) {
        Auditable auditable = clazz.getAnnotation(Auditable.class);
        if (auditable == null) {
            return clazz.getSimpleName() + " → 无 @Auditable 注解，无需审核";
        }
        return clazz.getSimpleName() + " → 审核人=" + auditable.reviewer()
                + ", 级别=" + auditable.level()
                + (auditable.level() > 1 ? " (需高级审核)" : " (普通审核)");
    }

    /**
     * 运行时读取 @Sensitive 注解，实现字段脱敏输出。
     * 模拟 Spring/Jackson 序列化时根据注解处理字段的逻辑。
     */
    public String processSensitive(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append("=== @Sensitive 脱敏处理: ").append(clazz.getSimpleName()).append(" ===\n");

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(obj);
            Sensitive sensitive = field.getAnnotation(Sensitive.class);

            if (sensitive != null) {
                String masked = mask((String) value, sensitive.type());
                sb.append("  ").append(field.getName()).append(": ")
                        .append(value).append(" → ").append(masked)
                        .append(" (类型=").append(sensitive.type()).append(")\n");
            } else {
                sb.append("  ").append(field.getName()).append(": ")
                        .append(value).append(" (非敏感)\n");
            }
        }
        return sb.toString();
    }

    private static String mask(String value, SensitiveType type) {
        if (value == null || value.isEmpty()) return value;
        switch (type) {
            case PHONE:
                return value.length() >= 7
                        ? value.substring(0, 3) + "****" + value.substring(value.length() - 4)
                        : "***";
            case EMAIL:
                int at = value.indexOf('@');
                return at > 2
                        ? value.substring(0, 2) + "***" + value.substring(at)
                        : "***";
            case MASK:
            default:
                return "***";
        }
    }

    /* ========== 统一入口 ========== */

    public static void main(String[] args) throws Exception {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();

        // 1. 类级别注解
        System.out.println(">>> @Auditable 类级别注解 <<<");
        System.out.println(demo.processAuditable(User.class));
        System.out.println(demo.processAuditable(String.class));
        System.out.println();

        // 2. 字段级别注解
        System.out.println(">>> @Sensitive 字段脱敏 <<<");
        User user = new User("张三", "13812345678", "zhangsan@company.com");
        System.out.println(demo.processSensitive(user));
    }
}
