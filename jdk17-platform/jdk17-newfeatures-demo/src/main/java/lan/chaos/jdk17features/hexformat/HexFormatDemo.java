package lan.chaos.jdk17features.hexformat;

import java.util.HexFormat;

/**
 * HexFormat（JDK17 新增工具类）：统一、可读、可定制的十六进制编解码，替代手写 {@code Integer.toHexString} 与位运算。
 *
 * <p>WHY：以前把 byte[] 转十六进制要么手写循环、要么引第三方库（如 commons-codec），且格式（分隔符/前缀/大小写）不好统一。
 * 关键 API：{@code HexFormat.of()}（紧凑）、{@code ofDelimiter(" ")}、{@code withPrefix("0x")}、{@code formatHex}/{@code parseHex}。
 */
public class HexFormatDemo {

    public static void run() {
        HexFormat fmt = HexFormat.of();
        byte[] data = {0x1, 0x2a, (byte) 0xff};

        String hex = fmt.formatHex(data);
        System.out.println("formatHex: " + hex);

        byte[] back = fmt.parseHex(hex);
        System.out.println("parseHex 还原长度: " + back.length);

        // 自定义：空格分隔 + 0x 前缀 + 大写
        HexFormat pretty = HexFormat.ofDelimiter(" ").withPrefix("0x").withUpperCase();
        System.out.println("美化: " + pretty.formatHex(data));
    }
}
