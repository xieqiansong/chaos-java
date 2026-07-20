package lan.chaos.distributed.system.distributed.id;

import redis.clients.jedis.Jedis;

public class RedisIdExample {
    public static void main(String[] args) {
        // 1. 连接Redis
        Jedis jedis = new Jedis("localhost", 30102);
        jedis.auth("REDACTED");
        
        // 2. 定义业务键
        String key = "order_id"; 

        // 3. 使用INCR命令原子性地自增并获取ID
        Long id = jedis.incr(key);
        System.out.println("Generated ID: " + id);

        // 4. 关闭连接
        jedis.close();
    }
}