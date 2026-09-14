package lan.chaos.microservice.order.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：扫描 order 的 Mapper，并挂分页插件。
 *
 * <p>WHY：order 是单数据源服务，Mapper 接口不写 @Mapper，统一由这里 @MapperScan 收口，避免散落。
 * 数据源本身已被 Seata 的「自动数据源代理」接管（见 application.yml 的 seata.enable-auto-data-source-proxy），
 * 所以 MP 拿到的就是 Seata 的 DataSourceProxy，AT 模式事务天然生效。</p>
 */
@Configuration
@MapperScan("lan.chaos.microservice.order.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
