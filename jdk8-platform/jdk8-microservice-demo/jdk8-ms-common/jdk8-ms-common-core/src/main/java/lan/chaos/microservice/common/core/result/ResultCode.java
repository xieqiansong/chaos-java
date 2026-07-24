package lan.chaos.microservice.common.core.result;

/**
 * 统一错误码。
 *
 * <p>WHY：微服务间、前后端需要一套稳定的“机器可读 + 人类可读”的返回码约定。
 * 业务异常 {@link lan.chaos.microservice.common.core.exception.BizException} 与全局异常处理器
 * 都复用这里的码，避免各服务各定义一套、前端无从适配。</p>
 *
 * <p>约定：0 表示成功；1 为通用失败；4xx 对应客户端问题（参数/未认证/无权限/不存在）；5xx 为服务端错误。</p>
 */
public enum ResultCode {

    SUCCESS(0, "success"),
    FAILED(1, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务器内部错误"),
    GATEWAY_BLOCKED(429, "请求过于频繁，已被限流"),
    SERVICE_DEGRADED(503, "下游服务暂时不可用，已降级");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
