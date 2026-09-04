package lan.chaos.java.base.annotation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * 自定义注解 + 运行时反射处理测试。
 */
class CustomAnnotationTest {

    @Test
    void auditable_shouldReadClassLevelAnnotation() {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();
        String result = demo.processAuditable(CustomAnnotationDemo.User.class);
        assertTrue(result.contains("admin"), "应读取审核人 admin");
        assertTrue(result.contains("级别=2"), "应读取级别 2");
        assertTrue(result.contains("高级审核"), "高级审核");
    }

    @Test
    void auditable_noAnnotation_shouldReturnDefaultMessage() {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();
        String result = demo.processAuditable(String.class);
        assertTrue(result.contains("无需审核"), "无注解应有提示");
    }

    @Test
    void sensitive_shouldMaskPhoneAndEmail() throws Exception {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();
        CustomAnnotationDemo.User user = new CustomAnnotationDemo.User("张三", "13812345678", "zhangsan@company.com");
        String result = demo.processSensitive(user);
        assertTrue(result.contains("张三") && result.contains("非敏感"), "name 非敏感");
        assertTrue(result.contains("****"), "phone 应脱敏");
        assertTrue(result.contains("***@"), "email 应脱敏");
    }
}
