package lan.chaos.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ServicePopupFix {

    public static void main(String[] args) throws Exception {
        // 使用WScript.Shell的Popup方法，显示是、否、取消按钮，并带有信息图标
        String psCommand = "powershell -Command \"$ws = New-Object -ComObject WScript.Shell; $result = $ws.Popup('番茄钟已结束！', 0, '专注结束', 68); Write-Output $result\"";
        
        Process process = Runtime.getRuntime().exec(psCommand);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        process.waitFor();
        
        int userChoice = 0;
        try {
            userChoice = Integer.parseInt(line.trim());
        } catch (Exception e) {
            // 处理异常
        }

        handleUserChoice(userChoice);
    }

    private static void handleUserChoice(int choice) {
        switch (choice) {
            case 6: // 是
                System.out.println("用户点击了是，开始休息...");
                // TODO: 你的业务逻辑
                break;
            case 7: // 否
                System.out.println("用户点击了否，继续专注...");
                // TODO: 你的业务逻辑
                break;
            case 2: // 取消
                System.out.println("用户点击了取消，不操作...");
                // TODO: 你的业务逻辑
                break;
            default:
                System.out.println("未知操作，code: " + choice);
        }
    }

}