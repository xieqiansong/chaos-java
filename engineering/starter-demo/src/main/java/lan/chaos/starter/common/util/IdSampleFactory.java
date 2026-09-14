package lan.chaos.starter.common.util;

import java.util.UUID;

/**
 * 样例数据工厂：让调用方无需自行准备输入即可把玩 starter。
 */
public final class IdSampleFactory {

    private IdSampleFactory() {
    }

    /** 生成一个样例 UUID 字符串（去掉横线，纯演示用）。 */
    public static String sampleUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
