package lan.chaos.filterasync.model;

/**
 * 上报体：只保留必要字段，避免全量反序列化。
 */
public class StatusReport {

    private String id;
    private String payload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
