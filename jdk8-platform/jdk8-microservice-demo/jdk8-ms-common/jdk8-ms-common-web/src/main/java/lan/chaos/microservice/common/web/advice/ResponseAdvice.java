package lan.chaos.microservice.common.web.advice;

import lan.chaos.microservice.common.core.result.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应包装：Controller 直接返回领域对象，在这里自动包成 {@link R}。
 *
 * <p>WHY：让业务代码只关心“返回什么数据”，不用每个接口手写 {@code return R.ok(x)}。
 * 已返回 {@link R} 的（异常处理器、或想自定义包装的）通过 {@link #supports} 跳过，避免双重包装。</p>
 *
 * <p>生产坑：{@code String} 类型直接返回会被 Spring 当成视图名，需特殊处理；
 * 本学习 Demo 统一返回对象，故不展开该分支。</p>
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 已经是 R 的不再包；其余（领域对象 / List / 基本类型）统一包成 R
        return !R.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof R) {
            return body;
        }
        if (body == null) {
            return R.ok();
        }
        return R.ok(body);
    }
}
