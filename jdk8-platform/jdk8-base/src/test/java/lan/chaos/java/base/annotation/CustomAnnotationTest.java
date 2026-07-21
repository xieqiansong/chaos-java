package lan.chaos.java.base.annotation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 自定义注解 + 运行时反射处理测试。
 */
public class CustomAnnotationTest {

    @Test
    public void auditable_shouldReadClassLevelAnnotation() {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();
        String result = demo.processAuditable(CustomAnnotationDemo.User.class);
        assertTrue("应读取审核人 admin", result.contains("admin"));
        assertTrue("应读取级别 2", result.contains("级别=2"));
        assertTrue("高级审核", result.contains("高级审核"));
    }

    @Test
    public void auditable_noAnnotation_shouldReturnDefaultMessage() {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();
        String result = demo.processAuditable(String.class);
        assertTrue("无注解应有提示", result.contains("无需审核"));
    }

    @Test
    public void sensitive_shouldMaskPhoneAndEmail() throws Exception {
        CustomAnnotationDemo demo = new CustomAnnotationDemo();
        CustomAnnotationDemo.User user = new CustomAnnotationDemo.User("张三", "13812345678", "zhangsan@company.com");
        String result = demo.processSensitive(user);
        assertTrue("name 非敏感", result.contains("张三") && result.contains("非敏感"));
        assertTrue("phone 应脱敏", result.contains("****"));
        assertTrue("email 应脱敏", result.contains("***@"));
    }
}
