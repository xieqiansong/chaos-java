package lan.chaos.tools;

public class Alert {
    public static void main(String[] args) throws Exception {
        // 最简单的系统提示音 + 弹窗
        Runtime.getRuntime().exec("rundll32 user32.dll,MessageBeep");
        Runtime.getRuntime().exec("cmd /c echo 任务完成 && pause > nul");
    }
}