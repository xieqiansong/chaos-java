package lan.chaos.websockettunnel.common.model;

import java.util.Objects;

/**
 * 从 WS 路径解析出的目标 TCP 地址。自带 {@link #sample()} 样例工厂，调用方无需自己造数据。
 */
public final class TargetAddress {

    private final String host;
    private final int port;

    public TargetAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TargetAddress sample() {
        return new TargetAddress("127.0.0.1", 3306);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /** 归一化表示，供白名单做精确匹配。 */
    public String key() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TargetAddress)) {
            return false;
        }
        TargetAddress that = (TargetAddress) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return key();
    }
}
