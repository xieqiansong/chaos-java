package lan.chaos.microservice.common.log.aspect;

import lan.chaos.microservice.common.log.annotation.Sensitive;
import lan.chaos.microservice.common.log.properties.AccessLogProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AccessLogAspect 单测：
 * <ol>
 *   <li>纯 {@code format(...)} 静态方法断言日志行结构；</li>
 *   <li>真正跑一遍 {@code around}，用 logback ListAppender 抓取 ACCESS_LOG 日志，验证脱敏与成败标记。</li>
 * </ol>
 * 不依赖 Spring 容器 / 中间件。
 */
class AccessLogAspectTest {

    static class LoginForm {
        String username = "admin";
        @Sensitive
        String password = "s3cret";
    }

    // 仅用于让 getSignature().getDeclaringType().getSimpleName() 返回可读类名
    static class UserController {}
    static class OrderController {}

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger accessLogger;

    private AccessLogAspect aspect(boolean includeArgs) {
        AccessLogProperties props = new AccessLogProperties();
        props.setIncludeArgs(includeArgs);
        return new AccessLogAspect(props);
    }

    private void attachAppender() {
        accessLogger = (Logger) LoggerFactory.getLogger("ACCESS_LOG");
        appender.start();
        accessLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (accessLogger != null) {
            accessLogger.detachAppender(appender);
        }
    }

    @Test
    void formatShouldContainSignatureElapsedAndOutcome() {
        String line = AccessLogAspect.format("trace-1", "UserController#login", "{username=admin}", 12, true, null);
        assertTrue(line.contains("[ACCESS]"));
        assertTrue(line.contains("trace=trace-1"));
        assertTrue(line.contains("UserController#login"));
        assertTrue(line.contains("elapsed=12ms"));
        assertTrue(line.contains("OK"));
    }

    @Test
    void formatShouldContainExceptionOnFailure() {
        String line = AccessLogAspect.format(null, "OrderController#create", "", 3, false, new RuntimeException("db down"));
        assertTrue(line.contains("FAIL"));
        assertTrue(line.contains("RuntimeException:db down"));
    }

    @Test
    void aroundShouldLogMaskedArgsAndOk() throws Throwable {
        attachAppender();
        ProceedingJoinPoint pjp = mockPjp("UserController#login", new LoginForm());
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect(true).around(pjp);

        assertEquals("ok", result);
        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        String msg = events.get(0).getFormattedMessage();
        assertTrue(msg.contains("UserController#login"));
        assertTrue(msg.contains("OK"));
        assertTrue(msg.contains("password=******"), "入参密码脱敏");
        assertFalse(msg.contains("s3cret"), "明文密码不得出现在日志");
    }

    @Test
    void aroundShouldLogFailAndRethrow() throws Throwable {
        attachAppender();
        ProceedingJoinPoint pjp = mockPjp("OrderController#create", "payload");
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> aspect(true).around(pjp));
        assertEquals("boom", ex.getMessage());

        String msg = appender.list.get(0).getFormattedMessage();
        assertTrue(msg.contains("FAIL"));
        assertTrue(msg.contains("IllegalStateException:boom"));
    }

    @Test
    void aroundShouldOmitArgsWhenDisabled() throws Throwable {
        attachAppender();
        ProceedingJoinPoint pjp = mockPjp("UserController#login", new LoginForm());
        when(pjp.proceed()).thenReturn("ok");

        aspect(false).around(pjp);

        String msg = appender.list.get(0).getFormattedMessage();
        assertFalse(msg.contains("password"), "关闭入参打印后不应出现字段");
    }

    private ProceedingJoinPoint mockPjp(String signature, Object arg) {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        String name = signature.split("#")[0];
        Class<?> declaring = name.equals("OrderController") ? OrderController.class : UserController.class;
        when(sig.getDeclaringType()).thenReturn(declaring);
        when(sig.getName()).thenReturn(signature.split("#")[1]);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[]{arg});
        return pjp;
    }
}
