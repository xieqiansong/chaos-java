package lan.chaos.microservice.common.web.advice;

import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>WHY：把“一切异常”收口成统一的 {@link R} 结构，前端永远拿到 {code,message,data}，
 * 而不是偶发的 500 堆栈或 Tomcat 默认错误页。分三类：</p>
 * <ul>
 *   <li>{@link BizException}：业务可预期错误，用其自带 code/message；</li>
 *   <li>{@link MethodArgumentNotValidException}：@Valid 参数校验失败，聚合成 “字段:原因”；</li>
 *   <li>其余 Exception：兜底为 500，避免敏感堆栈外泄。</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> ((FieldError) f).getField() + ":" + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return R.fail(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        // 未知异常按 5xx 处理；生产环境这里应告警，且不要把 e.getMessage() 直接回给前端
        log.error("未捕获异常", e);
        return R.fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage());
    }
}
