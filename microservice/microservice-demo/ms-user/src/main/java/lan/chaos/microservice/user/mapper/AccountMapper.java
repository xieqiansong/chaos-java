package lan.chaos.microservice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.microservice.user.entity.Account;

/**
 * 账户余额 Mapper（主库 PG）。由 {@code MybatisPlusConfig} 的 @MapperScan 扫描，无需 @Mapper。
 */
public interface AccountMapper extends BaseMapper<Account> {
}
