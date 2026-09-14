package lan.chaos.virtualthread.common.constant;

/**
 * 压测对照的两种执行器模式：
 * PLATFORM —— 平台线程池：固定 N 个 OS 线程，任务阻塞时线程被占住，超出的任务进队列排队。
 * VIRTUAL  —— 虚拟线程：每任务一个虚拟线程，阻塞即从载体线程卸载，不占 OS 线程。
 */
public enum ExecutorMode {

    PLATFORM("平台线程池"),
    VIRTUAL("虚拟线程");

    private final String desc;

    ExecutorMode(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }
}
