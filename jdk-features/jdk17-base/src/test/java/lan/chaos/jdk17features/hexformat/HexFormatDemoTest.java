package lan.chaos.jdk17features.hexformat;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HexFormatDemoTest {

    @Test
    void formatAndParse() {
        HexFormat fmt = HexFormat.of();
        assertEquals("01", fmt.formatHex(new byte[]{0x1}));
        assertEquals("012a", fmt.formatHex(new byte[]{0x1, 0x2a}));
        assertArrayEquals(new byte[]{0x1, 0x2a}, fmt.parseHex("012a"));
    }

    @Test
    void delimiterAndPrefix() {
        // withUpperCase 只大写十六进制数字，前缀 0x 保持原样
        HexFormat pretty = HexFormat.ofDelimiter(" ").withPrefix("0x").withUpperCase();
        assertEquals("0x01 0x2A", pretty.formatHex(new byte[]{0x1, 0x2a}));
    }
}
