package lan.chaos.demo.shortlink.idgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Snowflake ID 生成器
 * 结构: 1位符号位 + 41位时间戳(毫秒) + 10位机器ID + 12位序列号
 * 支持每毫秒 4096 个 ID，机器最多 1024 台
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /** 起始时间戳 (2023-01-01) */
    private static final long EPOCH = 1672531200000L;

    /** 机器 ID 位数 */
    private static final long WORKER_ID_BITS = 10L;
    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器 ID 最大值 1023 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    /** 序列号最大值 4095 */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 机器 ID 左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this.workerId = initWorkerId();
    }

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(String.format(
                    "Worker ID must be between 0 and %d", MAX_WORKER_ID));
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个唯一 ID
     */
    public synchronized long nextId() {
        long timestamp = currentTime();

        if (timestamp < lastTimestamp) {
            log.warn("Clock moved backwards, refusing to generate id for %d ms", lastTimestamp - timestamp);
            timestamp = waitUntilNext(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitUntilNext(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitUntilNext(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

    /**
     * 从机器硬件信息自动生成 Worker ID
     */
    private static long initWorkerId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                return 1L;
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return 1L;
            }
            long id = ((long) mac[mac.length - 1] & 0xFF)
                    | (((long) mac[mac.length - 2] & 0xFF) << 8);
            return id & MAX_WORKER_ID;
        } catch (Exception e) {
            log.warn("Failed to get MAC address, using PID-based worker ID", e);
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return (Long.parseLong(pid) % 1024) & MAX_WORKER_ID;
        }
    }

    public long getWorkerId() {
        return workerId;
    }
}
