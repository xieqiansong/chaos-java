package lan.chaos.mcp.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MCP 工具定义（服务端侧）。
 *
 * <p>WHY：与 Spring AI 本地工具（{@code @Tool}）的区别在于——这些工具运行在<b>独立进程</b>里，
 * 通过 MCP 协议被远程调用，因此可以被多个不同的 AI 应用共享，也可以单独部署、独立演进。</p>
 *
 * <p>{@code @McpTool} 的 description 决定客户端模型"何时调用"，
 * {@code @McpToolParam} 决定"传什么参数"，描述质量直接决定调用成功率。</p>
 */
@Component
public class DemoTools {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 无参工具：返回服务端当前时间（客户端模型本身不知道时间，必须借助工具）。 */
    @McpTool(name = "getServerTime", description = "获取 MCP 服务端的当前日期和时间")
    public String getServerTime() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /** 有参工具：演示参数抽取（模型需从自然语言中识别出城市名）。 */
    @McpTool(name = "getWeather", description = "查询指定城市的天气情况")
    public String getWeather(
            @McpToolParam(description = "城市名称，例如：北京、上海", required = true) String city) {
        // 演示用固定数据：真实场景应调用气象服务
        return String.format("%s：晴，25℃，湿度 40%%，东南风 2 级（演示数据）", city);
    }

    /** 多参工具：演示一次调用中抽取多个参数。 */
    @McpTool(name = "addNumbers", description = "计算两个整数的和")
    public int addNumbers(
            @McpToolParam(description = "第一个加数", required = true) int a,
            @McpToolParam(description = "第二个加数", required = true) int b) {
        return a + b;
    }
}
