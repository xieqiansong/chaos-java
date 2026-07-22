package lan.chaos.java.base.collection;


import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 集合源码分析测试：验证 HashMap / ConcurrentHashMap / ArrayList 的核心行为。
 */
public class CollectionSourceTest {

    @Test
    public void hashMap_putGet_thenResize() throws Exception {
        HashMapSourceAnalysis demo = new HashMapSourceAnalysis();
        String result = demo.demonstratePutGet();
        assertTrue("put 流程应包含 key=one", result.contains("one"));
        assertTrue("get 应命中", result.contains("O(1)"));
        assertTrue("应触发 resize", result.contains("resize"));
    }

    @Test
    public void hashMap_inspectInternal_shouldRevealTableAndThreshold() throws Exception {
        HashMapSourceAnalysis demo = new HashMapSourceAnalysis();
        String result = demo.inspectInternal();
        assertTrue("应显示 table.len", result.contains("table.len="));
        assertTrue("应显示 threshold", result.contains("threshold="));
        assertTrue("应显示 resize", result.contains("resize"));
    }

    @Test
    public void tableSizeFor_shouldReturnNextPowerOfTwo() {
        // 返回 >= cap 的最小 2 的幂：1->1, 2->2, 3->4, 10->16, 17->32（cap=0 兜底为 1）
        assertEquals(1, HashMapSourceAnalysis.tableSizeFor(1));
        assertEquals(2, HashMapSourceAnalysis.tableSizeFor(2));
        assertEquals(4, HashMapSourceAnalysis.tableSizeFor(3));
        assertEquals(16, HashMapSourceAnalysis.tableSizeFor(10));
        assertEquals(32, HashMapSourceAnalysis.tableSizeFor(17));
        assertEquals(1, HashMapSourceAnalysis.tableSizeFor(0));
    }

    @Test
    public void concurrentHashMap_concurrentPut_shouldNotLoseData() throws Exception {
        ConcurrentHashMapSourceAnalysis demo = new ConcurrentHashMapSourceAnalysis();
        String result = demo.concurrentPut();
        assertTrue("应无数据丢失", result.contains("✓ 无丢失无覆盖"));
        assertTrue("应能读取 key=0", result.contains("val-0"));
    }

    @Test
    public void concurrentHashMap_atomicOperations_shouldBeCorrect() {
        ConcurrentHashMapSourceAnalysis demo = new ConcurrentHashMapSourceAnalysis();
        String result = demo.atomicOperations();
        assertTrue("putIfAbsent 不应覆盖", result.contains("不会被覆盖"));
        assertTrue("merge 1+2+3", result.contains("6"));
    }

    @Test
    public void arrayList_shouldExpandAt15xRate() throws Exception {
        ArrayListSourceAnalysis demo = new ArrayListSourceAnalysis();
        String result = demo.demonstrateExpansion();
        assertTrue("应说明 1.5 倍增长", result.contains("1.5"));
        assertTrue("应显示扩容", result.contains("触发扩容"));
    }

    @Test
    public void arrayList_randomAccessShouldBeFasterThanLinkedList() throws Exception {
        ArrayListSourceAnalysis demo = new ArrayListSourceAnalysis();
        String result = demo.arrayListVsLinkedList();
        assertTrue("应说明 O(1) vs O(n)", result.contains("O(1)"));
        assertTrue("应说明 ArrayList get 更快", result.contains("ArrayList"));
    }
}
