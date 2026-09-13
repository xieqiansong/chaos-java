package lan.chaos.tools;

import javax.swing.JOptionPane;

public class SwingDialog {

    public static void main(String[] args) {
        // 1. 弹出弹窗并“阻塞”在这里，等待用户点击
        // 参数说明：父组件(null=居中), 提示内容, 标题, 按钮组类型, 图标类型
        int result = JOptionPane.showConfirmDialog(
            null, 
            "检测到新的更新，是否立即重启？", 
            "系统更新", 
            JOptionPane.YES_NO_CANCEL_OPTION, 
            JOptionPane.QUESTION_MESSAGE
        );

        // 2. 根据用户点击执行不同逻辑（这就是回调）
        if (result == JOptionPane.YES_OPTION) {
            System.out.println("用户点击了：是 (执行重启逻辑)");
        } else if (result == JOptionPane.NO_OPTION) {
            System.out.println("用户点击了：否 (执行稍后提醒逻辑)");
        } else if (result == JOptionPane.CANCEL_OPTION) {
            System.out.println("用户点击了：取消 (执行取消逻辑)");
        } else {
            // 用户直接点击了右上角的X关闭
            System.out.println("用户关闭了弹窗");
        }
    }
}