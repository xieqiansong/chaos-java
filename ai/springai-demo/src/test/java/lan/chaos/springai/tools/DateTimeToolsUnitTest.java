package lan.chaos.springai.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * chat/tools 纯单元测试：工具方法本身的正确性（不依赖模型/外部服务）。
 *
 * <p>WHY：工具是"被模型调用的业务方法"，本质是普通 Java 方法——
 * 它的逻辑正确性应该先由单测保证，再交给模型去编排，避免"模型调对了但方法算错了"。</p>
 */
class DateTimeToolsUnitTest {

    private final DateTimeTools tools = new DateTimeTools();

    @Test
    void currentDateTimeIsWellFormatted() {
        assertThat(tools.getCurrentDateTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void setAlarmEchoesGivenTime() {
        assertThat(tools.setAlarm("07:30")).isEqualTo("已将闹钟设置为 07:30");
    }
}
