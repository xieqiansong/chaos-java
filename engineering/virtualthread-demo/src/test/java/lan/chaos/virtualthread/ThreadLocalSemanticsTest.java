package lan.chaos.virtualthread;

import lan.chaos.virtualthread.threadlocal.ThreadLocalSemantics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ThreadLocal 语义断言：虚拟线程默认继承父线程的可继承上下文（与平台线程一致）；
 * 显式关闭继承（inheritInheritableThreadLocals(false)）后读不到父值。
 */
class ThreadLocalSemanticsTest {

    private final ThreadLocalSemantics semantics = new ThreadLocalSemantics();

    @Test
    void virtualThread_inheritsByDefault() throws Exception {
        assertEquals("ctx-42", semantics.readInChildByDefault("ctx-42"));
    }

    @Test
    void virtualThread_doesNotInherit_whenDisabled() throws Exception {
        assertNull(semantics.readInChildWithInheritanceDisabled("ctx-42"));
    }
}
