package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.audit.AuditScenario;
import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.common.context.TenantContext;
import lan.chaos.mybatisplus.dynamictable.DynamicTableScenario;
import lan.chaos.mybatisplus.encrypt.EncryptScenario;
import lan.chaos.mybatisplus.entity.User;
import lan.chaos.mybatisplus.page.PageScenario;
import lan.chaos.mybatisplus.tenant.TenantScenario;
import lan.chaos.mybatisplus.wrapper.WrapperScenario;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * MyBatis-Plus 高阶用法 Demo 入口。
 *
 * 设计取舍：演示逻辑放在 main() 里、在 Spring 上下文起来之后执行，而【不】用 @Bean CommandLineRunner。
 * 原因：@SpringBootTest 加载上下文时会自动执行所有 CommandLineRunner bean，若某个场景抛错，
 * 会导致「全部测试」上下文初始化失败、集体 error。把演示逻辑收进 main()，单测就能干净地加载上下文、
 * 直接调用各 Scenario 的公开方法做断言。
 *
 * 本类的 @SpringBootApplication 同时是各 *Test 的 @SpringBootTest 配置来源（组件扫描包 lan.chaos.mybatisplus）。
 */
@SpringBootApplication
@MapperScan("lan.chaos.mybatisplus.mapper")
public class DemoApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(DemoApp.class, args);
        try {
            demoRun(ctx);
        } finally {
            ctx.close();
        }
    }

    /** 控制台 Runner：逐场景打印「输入 → 输出」，方便直接 java -jar 把玩。 */
    private static void demoRun(ConfigurableApplicationContext ctx) {
        WrapperScenario wrapperScenario = ctx.getBean(WrapperScenario.class);
        PageScenario pageScenario = ctx.getBean(PageScenario.class);
        AuditScenario auditScenario = ctx.getBean(AuditScenario.class);
        TenantScenario tenantScenario = ctx.getBean(TenantScenario.class);
        DynamicTableScenario dynamicTableScenario = ctx.getBean(DynamicTableScenario.class);
        EncryptScenario encryptScenario = ctx.getBean(EncryptScenario.class);

        System.out.println("===== MyBatis-Plus 高阶用法 Demo =====");

        System.out.println("\n[1] Wrapper 高阶条件构造");
        List<User> complex = wrapperScenario.complexQuery();
        System.out.println("  complexQuery 命中 " + complex.size() + " 条: " + complex);
        List<User> custom = wrapperScenario.customSqlWithWrapper();
        System.out.println("  customSqlWithWrapper 命中 " + custom.size() + " 条: " + custom);

        System.out.println("\n[2] 分页（单表 + 联表）");
        System.out.println("  单表第1页(2条): " + pageScenario.userPage(1, 2).getRecords());
        System.out.println("  联表第1页(3条): " + pageScenario.orderUserPage(1, 3).getRecords());

        System.out.println("\n[3] 逻辑删除 + 乐观锁 + 自动填充");
        System.out.println("  " + auditScenario.logicDeleteAndVersion());

        System.out.println("\n[4] 多租户隔离");
        System.out.println("  " + tenantScenario.tenantIsolation());
        TenantContext.clear();

        System.out.println("\n[5] 动态表名分表");
        System.out.println("  " + dynamicTableScenario.routeByYear());
        DynamicTableContext.clear();

        System.out.println("\n[6] 字段 AES 透明加密");
        System.out.println("  " + encryptScenario.transparentEncrypt());
    }
}
