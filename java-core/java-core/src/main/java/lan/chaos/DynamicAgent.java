package lan.chaos;

import java.lang.instrument.Instrumentation;

public class DynamicAgent {
    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[Agent] agentmain called, args: " + args);

        // 示例：打印当前已加载的类数量
        System.out.println("[Agent] Loaded classes count: " 
            + inst.getAllLoadedClasses().length);
    }
}