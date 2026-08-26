package lan.chaos.hmac.model;

/**
 * 设备上报请求（去隐私泛化模型）。
 * <ul>
 *   <li>{@code deviceId}：设备 ID（10~15 字母数字）</li>
 *   <li>{@code path}：接口路径</li>
 *   <li>{@code timestamp}：请求时间戳（秒）</li>
 *   <li>{@code nonce}：一次性随机串（防签名重放，配合时间窗）</li>
 *   <li>{@code batchNo}：业务批次号，deviceId+batchNo 构成幂等唯一键（写侧去重兜底）</li>
 *   <li>{@code body}：请求体（参与签名，防篡改）</li>
 *   <li>{@code sign}：HMAC-SHA256 签名</li>
 * </ul>
 */
public final class ReportRequest {

    private String deviceId;
    private String path;
    private long timestamp;
    private String nonce;
    private String batchNo;
    private String body;
    private String sign;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
