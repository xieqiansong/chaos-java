package lan.chaos.jvm.gc;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * GC 演示测试：验证 GC 探测、Minor GC 模拟、Full GC 模拟、日志指南的正确性。
 */
public class GarbageCollectorDemoTest {

    @Test
    public void detectCurrentGC_shouldShowGCInfo() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.detectCurrentGC();
        assertTrue("应显示 GC 收集器名称", result.contains("GC 收集器"));
        assertTrue("应显示堆内存", result.contains("堆内存"));
        assertTrue("应显示非堆", result.contains("非堆"));
    }

    @Test
    public void simulateMinorGC_shouldCompleteWithoutOOM() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.simulateMinorGC();
        assertTrue("应包含 Minor GC 字样", result.contains("Minor GC"));
        assertTrue("应显示 GC 次数", result.contains("GC 次数"));
    }

    @Test
    public void simulateFullGC_shouldCompleteWithoutOOM() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.simulateFullGC();
        assertTrue("应包含 Full GC 字样", result.contains("Full GC"));
        assertTrue("应显示强引用说明", result.contains("强引用"));
    }

    @Test
    public void gcLogGuide_shouldIncludeKeyParameters() {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        String result = demo.gcLogGuide();
        assertTrue("应包含 PrintGCDetails", result.contains("PrintGCDetails"));
        assertTrue("应包含 Xms", result.contains("Xms"));
        assertTrue("应包含 G1GC", result.contains("G1GC"));
        assertTrue("应包含 PSYoungGen", result.contains("PSYoungGen"));
    }
}
