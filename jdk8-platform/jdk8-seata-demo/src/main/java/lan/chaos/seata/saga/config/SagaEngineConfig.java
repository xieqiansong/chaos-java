package lan.chaos.seata.saga.config;

import io.seata.rm.datasource.DataSourceProxy;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.impl.DefaultStateMachineConfig;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import io.seata.saga.engine.serializer.impl.ExceptionSerializer;
import io.seata.saga.engine.serializer.impl.ParamsSerializer;
import io.seata.saga.engine.store.db.DbAndReportTcStateLogStore;
import io.seata.saga.engine.store.db.DbStateLangStore;
import io.seata.saga.tm.DefaultSagaTransactionalTemplate;
import io.seata.saga.tm.SagaTransactionalTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * SAGA 官方状态机引擎装配（seata-saga-statelang，内置于 seata-all 1.6.1）。
 *
 * <p>仅在 {@code seata.enabled=true}（连接真实 Seata Server）时激活；
 * 单测环境 {@code seata.enabled=false} 不会加载，避免无 TC 时初始化引擎。</p>
 *
 * <p>装配要点：</p>
 * <ul>
 *   <li>{@link DefaultStateMachineConfig}：引擎配置，启动时解析 {@code classpath*:saga/*.json}
 *       并把状态机定义写入 {@code seata_state_machine_def}（自动注册，InitializingBean）。</li>
 *   <li>{@link DbStateLangStore}：状态机定义落库（原始数据源，避免被 AT 代理拦截）。</li>
 *   <li>{@link DbAndReportTcStateLogStore}：状态机/状态实例落库，并通过
 *       {@link SagaTransactionalTemplate} 与 TC 通信（分支注册 / 全局事务提交回滚）。</li>
 * </ul>
 *
 * @author chaos
 */
@Configuration
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
public class SagaEngineConfig {

    /**
     * 状态机引擎 Bean：业务侧通过 {@code engine.start("stockPurchaseSaga", null, params)} 驱动。
     */
    @Bean
    public StateMachineEngine stateMachineEngine(StateMachineConfig stateMachineConfig) {
        ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
        engine.setStateMachineConfig(stateMachineConfig);
        return engine;
    }

    /**
     * 引擎配置：挂接 JSON 资源与 DB 存储。
     */
    @Bean
    public StateMachineConfig stateMachineConfig(DataSource dataSource, Environment environment) {
        DefaultStateMachineConfig config = new DefaultStateMachineConfig();
        config.setResources(new String[]{"classpath*:saga/*.json"});
        config.setEnableAsync(false);
        config.setSagaJsonParser("fastjson");
        config.setDefaultTenantId("000001");

        // SAGA 引擎的日志表不应被 AT 代理拦截：使用被代理前的原始数据源
        DataSource raw = unwrapIfProxy(dataSource);

        DbStateLangStore stateLangStore = new DbStateLangStore();
        stateLangStore.setDataSource(raw);
        stateLangStore.setTablePrefix("seata_");
        config.setStateLangStore(stateLangStore);

        DbAndReportTcStateLogStore stateLogStore = new DbAndReportTcStateLogStore();
        stateLogStore.setDataSource(raw);
        stateLogStore.setTablePrefix("seata_");
        stateLogStore.setDefaultTenantId("000001");
        stateLogStore.setSeqGenerator(config.getSeqGenerator());
        stateLogStore.setSagaTransactionalTemplate(sagaTransactionalTemplate(environment));

        ParamsSerializer paramsSerializer = new ParamsSerializer();
        paramsSerializer.setJsonParserName("fastjson");
        stateLogStore.setParamsSerializer(paramsSerializer);
        stateLogStore.setExceptionSerializer(new ExceptionSerializer());

        config.setStateLogStore(stateLogStore);
        return config;
    }

    /**
     * SAGA 与 TC 通信模板：向 TC 注册/报告分支事务、提交/回滚全局事务。
     * <p>TM / RM 客户端由 seata-spring-boot-starter 已初始化，此处仅提供连接参数。</p>
     */
    private SagaTransactionalTemplate sagaTransactionalTemplate(Environment environment) {
        DefaultSagaTransactionalTemplate template = new DefaultSagaTransactionalTemplate();
        template.setApplicationId(environment.getProperty("spring.application.name", "jdk8-seata-demo"));
        template.setTxServiceGroup(environment.getProperty("seata.tx-service-group", "chaos-seata-tx-group"));
        return template;
    }

    private DataSource unwrapIfProxy(DataSource dataSource) {
        if (dataSource instanceof DataSourceProxy) {
            return ((DataSourceProxy) dataSource).getTargetDataSource();
        }
        return dataSource;
    }
}
