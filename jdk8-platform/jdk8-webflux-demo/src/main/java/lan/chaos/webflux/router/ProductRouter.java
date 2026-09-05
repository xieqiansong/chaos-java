package lan.chaos.webflux.router;

import lan.chaos.webflux.common.constant.ApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * 能力三：RouterFunction 函数式路由。
 *
 * <p>WHY：函数式端点是 WebFlux 对 @Controller 注解的替代——路由与处理都是「函数」，
 * route().GET(...).POST(...) 链式声明，类型安全、可组合、易测试，不走反射/注解扫描。
 * 适合「端点少、要极致可控 / 可拼装」的场景。底层仍是同一套 WebFlux 引擎。
 */
@Configuration
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return RouterFunctions.route()
                .GET(ApiConstants.PRODUCTS, accept(MediaType.APPLICATION_JSON), handler::list)
                .GET(ApiConstants.PRODUCT_BY_ID, accept(MediaType.APPLICATION_JSON), handler::getById)
                .POST(ApiConstants.PRODUCTS, accept(MediaType.APPLICATION_JSON), handler::create)
                .build();
    }
}
