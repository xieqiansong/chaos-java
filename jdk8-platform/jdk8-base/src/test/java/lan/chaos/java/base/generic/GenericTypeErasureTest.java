package lan.chaos.java.base.generic;


import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

/**
 * 泛型类型擦除测试：验证泛型在编译期擦除、运行时无法区分的核心行为。
 */
public class GenericTypeErasureTest {

    @Test
    public void sameClass_afterErasure_listStringAndListInteger() {
        GenericTypeErasureDemo demo = new GenericTypeErasureDemo();
        String result = demo.trapSameClass();
        assertTrue("List<String> 和 List<Integer> Class 应相同", result.contains("两者 Class 相同? true"));
        assertTrue("应说明类型已擦除", result.contains("类型已擦除"));
    }

    @Test
    public void instanceof_onlyWorksWithRawType() {
        GenericTypeErasureDemo demo = new GenericTypeErasureDemo();
        String result = demo.trapInstanceof();
        assertTrue("instanceof 只能用原始类型", result.contains("true")
                || result.contains("List"));
        assertTrue("应提到编译错误", result.contains("编译错误"));
    }

    @Test
    public void genericArray_creationIsRestricted() {
        GenericTypeErasureDemo demo = new GenericTypeErasureDemo();
        String result = demo.trapGenericArray();
        assertTrue("应显示变通方案", result.contains("hello"));
        assertTrue("应推荐 safe 方案", result.contains("better"));
    }

    @Test
    public void typeToken_capturesGenericViaAnonymousSubclass() {
        GenericTypeErasureDemo demo = new GenericTypeErasureDemo();
        String result = demo.typeTokenPrinciple();
        assertTrue("匿名子类应捕获泛型 String", result.contains("String"));
        assertTrue("普通 ArrayList 无法获取", result.contains("无法获取") || result.contains("String"));
    }

    @Test
    public void pecs_producerExtendsConsumerSuper() {
        GenericTypeErasureDemo demo = new GenericTypeErasureDemo();
        String result = demo.pecsPrinciple();
        assertTrue("应包含 PECS 原则", result.contains("Producer Extends, Consumer Super"));
        assertTrue("extends 应可 get", result.contains("Producer"));
    }
}
