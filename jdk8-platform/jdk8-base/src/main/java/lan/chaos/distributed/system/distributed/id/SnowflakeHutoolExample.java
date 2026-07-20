package lan.chaos.distributed.system.distributed.id;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public class SnowflakeHutoolExample {
    public static void main(String[] args) {
        // 参数1为终端ID (workerId), 参数2为数据中心ID (datacenterId)[reference:3]
        // 在分布式环境中，这两个参数需要保证唯一
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        
        // 生成ID
        long id = snowflake.nextId();
        System.out.println("Generated ID: " + id);
    }
}