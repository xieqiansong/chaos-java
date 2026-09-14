package lan.chaos.demo.shortlink.util;

/**
 * Base62 编解码工具
 * 将数字 ID 转换为 [0-9a-zA-Z] 短字符串，用于短链生成
 */
public final class Base62Util {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final char[] CHARS = BASE62.toCharArray();
    private static final int BASE = 62;

    private Base62Util() {}

    /**
     * 将 long 型 ID 编码为 Base62 字符串
     */
    public static String encode(long num) {
        if (num == 0) {
            return String.valueOf(CHARS[0]);
        }
        StringBuilder sb = new StringBuilder();
        long n = num;
        while (n > 0) {
            sb.append(CHARS[(int) (n % BASE)]);
            n /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * 将 Base62 字符串解码为 long 型 ID
     */
    public static long decode(String str) {
        long num = 0;
        for (char c : str.toCharArray()) {
            num = num * BASE + BASE62.indexOf(c);
        }
        return num;
    }
}
