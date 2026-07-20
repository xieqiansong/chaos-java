package lan.chaos.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局插件配置（高阶核心）。
 * 插件执行顺序很关键：分页在最前改写 SQL，其后的多租户 / 动态表名在改写后的 SQL 上继续注入，
 * 乐观锁与防全表更新放最后。顺序错乱会导致生成的 SQL 不符合预期。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1) 分页插件（放最前）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));

        // 2) 多租户：仅对 tenant_data 注入 tenant_id 条件（由 MyTenantLineHandler.ignoreTable 控制）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new MyTenantLineHandler()));

        // 3) 动态表名：log_record -> log_record_${suffix}
        DynamicTableNameInnerInterceptor dynamicTable = new DynamicTableNameInnerInterceptor();
        dynamicTable.setTableNameHandler(new MyDynamicTableNameHandler());
        interceptor.addInnerInterceptor(dynamicTable);

        // 4) 乐观锁：更新自动拼接 version 条件并自增，version 不一致则更新失败
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 5) 防全表更新/删除：无 WHERE 的 update/delete 直接抛异常，避免误操作
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
}
