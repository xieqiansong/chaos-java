package lan.chaos.microservice.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.microservice.order.model.Order;

/**
 * 订单 Mapper（MySQL）。由 {@code MybatisPlusConfig} 的 @MapperScan 扫描，无需 @Mapper。
 */
public interface OrderMapper extends BaseMapper<Order> {
}
