package lan.chaos.microservice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.microservice.user.entity.User;

/**
 * 用户 Mapper（主库 PG）。无 @DS → 走 dynamic-datasource 的 primary 数据源。
 */
public interface UserMapper extends BaseMapper<User> {
}
