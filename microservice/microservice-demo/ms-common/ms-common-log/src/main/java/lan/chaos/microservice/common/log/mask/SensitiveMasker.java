package lan.chaos.microservice.common.log.mask;

import lan.chaos.microservice.common.log.annotation.Sensitive;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ★★★ P5 访问日志脱敏工具：把一个对象转成「可安全打印」的字符串，敏感字段 / 键一律打码。
 *
 * <p>WHY：访问日志要把请求入参打出来方便排障，但密码、token、密钥一旦落盘就是安全事故。
 * 这里做统一脱敏——命中敏感字段名（或 {@link Sensitive} 注解）的值替换为 {@code ******}，
 * 既保留可观测性又不泄露凭据。下游访问日志切面直接复用本类，因此它被写成纯函数、极易单测。</p>
 *
 * <p>覆盖类型：Map（按 key 名判定）、POJO（按字段名 / 注解）、Collection / 数组（递归逐元素）、
 * 基础类型与 String 原样返回。对 Servlet / Spring 框架对象（如 HttpServletRequest、BindingResult）
 * 直接输出类型占位，避免把一整坨不可序列化的对象强转成字符串。</p>
 */
public final class SensitiveMasker {

    /** 打码占位 */
    private static final String MASK = "******";
    /** 单条日志最大长度，超出截断，避免大报文刷屏 */
    private static final int MAX_LEN = 256;
    /** 脱敏递归最大深度，防环 / 超大对象 */
    private static final int MAX_DEPTH = 4;

    /** 命中即打码的字段名 / key（大小写不敏感，按包含匹配，容忍 userPassword 这类拼写） */
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "pwd", "passwd", "pass", "token", "secret", "secrets",
            "authorization", "auth", "credentials", "credential", "apikey", "api_key",
            "accesskey", "access_key", "privatekey", "private_key", "certificate", "cert", "salt"));

    /** 直接输出类型占位的框架对象包前缀 */
    private static final Set<String> SKIP_PREFIXES = new HashSet<>(Arrays.asList(
            "javax.servlet.", "jakarta.servlet.", "org.springframework.validation.",
            "org.springframework.ui.", "org.springframework.http.", "org.springframework.web.",
            "java.io.", "java.nio."));

    private SensitiveMasker() {
    }

    public static String mask(Object value) {
        return doMask(value, new HashSet<>(), 0);
    }

    private static String doMask(Object value, Set<Integer> seen, int depth) {
        if (value == null) {
            return "null";
        }
        if (depth > MAX_DEPTH) {
            return "...";
        }
        if (value instanceof String) {
            return cap((String) value);
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return cap(String.valueOf(value));
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (!seen.add(System.identityHashCode(map))) {
                return "{...}";
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                String key = String.valueOf(e.getKey());
                Object v = e.getValue();
                sb.append(key).append("=").append(isSensitiveKey(key) ? MASK : doMask(v, seen, depth + 1));
            }
            return sb.append("}").toString();
        }
        if (value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            if (!seen.add(System.identityHashCode(col))) {
                return "[...]";
            }
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object e : col) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(doMask(e, seen, depth + 1));
            }
            return sb.append("]").toString();
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            if (!seen.add(System.identityHashCode(value))) {
                return "[...]";
            }
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (i > 50) {
                    sb.append("...");
                    break;
                }
                sb.append(doMask(Array.get(value, i), seen, depth + 1));
            }
            return sb.append("]").toString();
        }
        // 其余对象：反射字段，命中敏感名或 @Sensitive 打码
        Class<?> type = value.getClass();
        if (isSkipped(type)) {
            return "<" + type.getSimpleName() + ">";
        }
        if (!seen.add(System.identityHashCode(value))) {
            return "<" + type.getSimpleName() + " ...>";
        }
        StringBuilder sb = new StringBuilder(type.getSimpleName()).append("{");
        boolean first = true;
        for (Field f : allFields(type)) {
            f.setAccessible(true);
            Object fv;
            try {
                fv = f.get(value);
            } catch (Exception ex) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            first = false;
            boolean sensitive = isSensitiveKey(f.getName()) || f.isAnnotationPresent(Sensitive.class);
            sb.append(f.getName()).append("=").append(sensitive ? MASK : doMask(fv, seen, depth + 1));
        }
        return sb.append("}").toString();
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        for (String s : SENSITIVE_KEYS) {
            if (lower.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSkipped(Class<?> type) {
        String name = type.getName();
        for (String prefix : SKIP_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static java.util.List<Field> allFields(Class<?> type) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        Class<?> c = type;
        while (c != null && c != Object.class) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
            c = c.getSuperclass();
        }
        return fields;
    }

    private static String cap(String s) {
        if (s == null) {
            return "null";
        }
        if (s.length() <= MAX_LEN) {
            return s;
        }
        return s.substring(0, MAX_LEN) + "...(truncated)";
    }
}
