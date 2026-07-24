package lan.chaos.microservice.common.core.result;

/**
 * 统一响应体。
 *
 * <p>WHY：所有 HTTP 接口返回同一信封结构 {code, message, data, timestamp}，
 * 前端/调用方只需关注 code 与 data。配合 {@code common-web} 的 {@code ResponseAdvice}，
 * Controller 直接返回领域对象即可被自动包成 {@code R}，无需每个接口手写包装。</p>
 *
 * <p>注意：{@code timestamp} 用毫秒时间戳，避免时区/序列化歧义；{@code code=0} 视为成功。</p>
 */
public class R<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(ResultCode.FAILED.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
