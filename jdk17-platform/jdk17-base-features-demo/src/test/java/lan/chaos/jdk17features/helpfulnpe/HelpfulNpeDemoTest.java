package lan.chaos.jdk17features.helpfulnpe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelpfulNpeDemoTest {

    @Test
    void detailedNpeMessage() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> {
            HelpfulNpeDemo.A a = new HelpfulNpeDemo.A();
            String ignore = a.b.c.name; // a.b.c 为 null
        });
        // JDK17 默认开启精确 NPE，信息会指出是哪个引用为 null
        assertNotNull(ex.getMessage());
        System.out.println("捕获 NPE 信息: " + ex.getMessage());
    }
}
