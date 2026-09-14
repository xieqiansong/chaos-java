package lan.chaos.microservice.common.core.exception;

import lan.chaos.microservice.common.core.result.ResultCode;

/**
 * 业务异常。
 *
 * <p>WHY：比起散落各处的 {@code throw new RuntimeException("xxx")}，统一业务异常让全局异常处理器
 * 能精确捕获并转成 {@link lan.chaos.microservice.common.core.result.R#fail(int, String)}，
 * 把“业务可预期的错误”和“程序 bug”区分开，前端拿到的是结构化错误而非 500 堆栈。</p>
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
