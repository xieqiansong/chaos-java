package lan.chaos.microservice.common.feign.fallback;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;

/**
 * 降级响应工厂。全部返回约定好的 {@link ResultCode#SERVICE_DEGRADED}（503）{@code R}，
 * 保证所有 Feign 兜底响应结构一致、前端/调用方可统一识别“这是降级而非业务失败”。
 */
public final class FallbackResults {

    private FallbackResults() {
    }

    /** 通用降级响应 */
    public static <T> R<T> degraded() {
        return R.fail(ResultCode.SERVICE_DEGRADED);
    }

    /** 带细节的降级响应（detail 会拼到 message 后，便于定位是哪个方法降级了） */
    public static <T> R<T> degraded(String detail) {
        return R.fail(ResultCode.SERVICE_DEGRADED.getCode(),
                ResultCode.SERVICE_DEGRADED.getMessage() + "：" + detail);
    }
}
