package lan.chaos.proxy;

/**
 * 被懒加载代理的目标类：自身构造很廉价，
 * 「昂贵的资源准备」放到 LazyLoader 的 loadObject 里（首次方法调用时才执行）。
 */
public class HeavyService {

    public HeavyService() {
        // 注意：这里不做事，保证代理对象自身构造是廉价的
    }

    public String work() {
        return "heavy work done";
    }
}
