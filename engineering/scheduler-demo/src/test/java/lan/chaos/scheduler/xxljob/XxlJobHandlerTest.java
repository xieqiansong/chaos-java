package lan.chaos.scheduler.xxljob;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XXL-JOB 验证（无需外部 admin）：
 * 用反射确认 {@link SampleXxlJobHandler} 中标注了 {@link XxlJob} 的任务处理器，
 * 任务名与 demo 约定一致。真正的「向 admin 注册并执行」需要外部调度中心，
 * 属集成环境，不在本单元测试范围。
 */
class XxlJobHandlerTest {

    @Test
    void handlerExposesExpectedXxlJobMethods() {
        Method[] methods = SampleXxlJobHandler.class.getDeclaredMethods();
        List<String> jobNames = new ArrayList<>();
        for (Method m : methods) {
            XxlJob anno = m.getAnnotation(XxlJob.class);
            if (anno != null) jobNames.add(anno.value());
        }

        assertTrue(jobNames.contains("schedulerDemoShardJob"),
                "应包含分片任务 schedulerDemoShardJob");
        assertTrue(jobNames.contains("schedulerDemoSimpleJob"),
                "应包含简单任务 schedulerDemoSimpleJob");
    }
}
