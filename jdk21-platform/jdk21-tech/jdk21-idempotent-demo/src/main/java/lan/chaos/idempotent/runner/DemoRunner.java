package lan.chaos.idempotent.runner;

import lan.chaos.idempotent.common.constant.Scenario;
import lan.chaos.idempotent.demo.ConcurrentDoubleSubmitDemo;
import lan.chaos.idempotent.demo.ConsumeDedupDemo;
import lan.chaos.idempotent.demo.StateMachineDedupDemo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动主线：串联三层幂等防护，分节打印「输入→输出」。
 * 顺序：请求级（并发双发）→ 消费级（重复投递）→ 状态机级（重复回调）。
 */
@Slf4j
@Component
public class DemoRunner implements ApplicationRunner {

    private final ConcurrentDoubleSubmitDemo requestDemo;
    private final ConsumeDedupDemo consumeDemo;
    private final StateMachineDedupDemo stateDemo;

    public DemoRunner(ConcurrentDoubleSubmitDemo requestDemo,
                      ConsumeDedupDemo consumeDemo,
                      StateMachineDedupDemo stateDemo) {
        this.requestDemo = requestDemo;
        this.consumeDemo = consumeDemo;
        this.stateDemo = stateDemo;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 接口幂等 Demo 开始（请求级 / 消费级 / 状态机级三层去重）=====");
        log.info("主线认知：幂等 = at-least-once 语义下的「重复发生也不重复副作用」，分三层去重");

        log.info("--- [{}] {} ---", Scenario.REQUEST, Scenario.REQUEST.desc());
        log.info("[输出] {}", requestDemo.run());

        log.info("--- [{}] {} ---", Scenario.CONSUME, Scenario.CONSUME.desc());
        log.info("[输出] {}", consumeDemo.run());

        log.info("--- [{}] {} ---", Scenario.STATE, Scenario.STATE.desc());
        log.info("[输出] {}", stateDemo.run());

        log.info("===== 接口幂等 Demo 结束 =====");
    }
}
