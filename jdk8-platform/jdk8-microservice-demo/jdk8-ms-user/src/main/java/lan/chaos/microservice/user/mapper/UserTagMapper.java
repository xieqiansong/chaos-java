package lan.chaos.microservice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.microservice.user.entity.UserTag;

/**
 * 用户标签 Mapper（副库 MySQL）。同级 service 用 @DS("mysql") 路由到该数据源。
 */
public interface UserTagMapper extends BaseMapper<UserTag> {
}
