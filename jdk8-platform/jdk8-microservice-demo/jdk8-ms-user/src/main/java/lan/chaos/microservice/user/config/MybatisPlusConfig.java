package lan.chaos.microservice.user.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：分页插件 + 自动填充审计字段。
 *
 * <p>WHY：</p>
 * <ul>
 *   <li>分页拦截器把 {@code Page} 自动翻译成对应数据库的 limit/offset（这里按主库 PG 设置方言）；</li>
 *   <li>{@link MetaObjectHandler} 在 insert/update 时自动写入 created_at / updated_at，
 *   业务代码不手写时间，规避“忘记更新 updated_at”这类低级 bug。</li>
 * </ul>
 *
 * <p>生产坑（多数据源）：分页方言按“主数据源”设置即可覆盖绝大多数场景；若副库 MySQL 也要分页且表很大，
 * 应考虑为每个数据源单独配拦截器或使用 dynamic-datasource 的 {@code @DS} + 读写分离插件，本 P1 先不展开。</p>
 */
@Configuration
@MapperScan("lan.chaos.microservice.user.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(com.baomidou.mybatisplus.annotation.DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                setFieldValByName("createdAt", now, metaObject);
                setFieldValByName("updatedAt", now, metaObject);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
            }
        };
    }
}
