package lan.chaos.nacos.common;

import java.io.Serializable;

/**
 * 服务提供方与消费方共享的传输实体。
 *
 * <p>放在公共模块，避免 provider/consumer 各自重复定义 DTO。</p>
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    /**
     * 处理该请求的服务实例标识（如 ip:port），用于消费端观察负载均衡效果。
     */
    private String servedBy;

    public User() {
    }

    public User(Long id, String name, String servedBy) {
        this.id = id;
        this.name = name;
        this.servedBy = servedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServedBy() {
        return servedBy;
    }

    public void setServedBy(String servedBy) {
        this.servedBy = servedBy;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', servedBy='" + servedBy + "'}";
    }
}
