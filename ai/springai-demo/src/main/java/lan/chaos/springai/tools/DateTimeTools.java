package lan.chaos.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工具定义：供模型调用的本地方法（Function Calling 的"函数"）。
 *
 * <p>WHY：{@code @Tool} 是 Spring AI 1.0+ 的声明式工具定义方式（取代了早期的 {@code @Function}）。
 * 其中 {@code description} 决定模型"何时调用它"，{@code @ToolParam} 决定"传什么参数"——
 * 描述写得越准确，模型选对工具、填对参数的概率越高，这是工具调用能否work的关键。</p>
 */
public class DateTimeTools {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 无参工具：获取当前日期时间。 */
    @Tool(description = "获取当前的日期和时间，精确到秒")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /** 有参工具：演示模型从自然语言中抽取参数（如"早上七点半叫我" → "07:30"）。 */
    @Tool(description = "设置一个指定时间的闹钟")
    public String setAlarm(@ToolParam(description = "24小时制时间，格式为 HH:mm") String time) {
        return "已将闹钟设置为 " + time;
    }
}
