package lan.chaos.jvm.gc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GC 演示测试：验证 GC 探测、Minor GC 模拟、Full GC 模拟、日志指南的正确性。
 */
public class GarbageCollectorDemoTest {

    @Test
    public void detectCurrentGC_shouldShowGCInfo() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.detectCurrentGC();
        assertTrue(result.contains("GC 收集器"), "应显示 GC 收集器名称");
        assertTrue(result.contains("堆内存"), "应显示堆内存");
        assertTrue(result.contains("非堆"), "应显示非堆");
    }

    @Test
    public void simulateMinorGC_shouldCompleteWithoutOOM() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.simulateMinorGC();
        assertTrue(result.contains("Minor GC"), "应包含 Minor GC 字样");
        assertTrue(result.contains("GC 次数"), "应显示 GC 次数");
    }

    @Test
    public void simulateFullGC_shouldCompleteWithoutOOM() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.simulateFullGC();
        assertTrue(result.contains("Full GC"), "应包含 Full GC 字样");
        assertTrue(result.contains("强引用"), "应显示强引用说明");
    }

    @Test
    public void gcLogGuide_shouldIncludeKeyParameters() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.gcLogGuide();
        assertTrue(result.contains("PrintGCDetails"), "应包含 PrintGCDetails");
        assertTrue(result.contains("Xms"), "应包含 Xms");
        assertTrue(result.contains("G1GC"), "应包含 G1GC");
        assertTrue(result.contains("PSYoungGen"), "应包含 PSYoungGen");
    }
}
