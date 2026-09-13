package lan.chaos.tools;

import java.io.IOException;

public class WindowsNotify {

    /**
     * 显示 Windows 系统通知（弹窗）
     *
     * @param message 通知内容
     */
    public static void show(String message) {
        show(message, 5);
    }

    /**
     * 显示 Windows 系统通知（弹窗）
     *
     * @param message 通知内容
     * @param seconds 显示时长（秒）
     */
    public static void show(String message, int seconds) {
        try {
            String cmd = String.format(
                    "cmd /c msg * /time:%d \"%s\"",
                    seconds,
                    message.replace("\"", "\\\"")
            );

            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示带标题的通知
     *
     * @param title   标题
     * @param message 内容
     */
    public static void info(String title, String message) {
        show(title + "\n" + message);
    }

    /**
     * 显示警告通知
     */
    public static void warning(String title, String message) {
        try {
            Runtime.getRuntime().exec(
                    "cmd /c msg * /time:5 \"" + title + "\n⚠ " + message + "\""
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示错误通知
     */
    public static void error(String title, String message) {
        try {
            Runtime.getRuntime().exec(
                    "cmd /c msg * /time:8 \"" + title + "\n❌ " + message + "\""
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 测试
    public static void main(String[] args) {
        info("系统提示", "服务启动成功！");

        warning("警告", "内存占用过高");

        error("错误", "数据库连接失败");

        show("自定义消息\n第二行内容", 3);
    }
}