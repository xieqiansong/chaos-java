package lan.chaos.finance.common.runner;

import lan.chaos.finance.money.MoneyArithmetic;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 控制台演示触发器（非知识点本身）：应用启动时按顺序打印各能力包场景的「输入 → 输出」。
 * 仅用于把玩；语义正确性由 {@code *Test} 断言保证。
 */
@Component
@org.springframework.core.annotation.Order(1)
public class MoneyDemoRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("===== [money] 金额运算演示（P0 地基） =====");
        MoneyArithmetic.doubleTrap();
        MoneyArithmetic.construct();
        MoneyArithmetic.addSubtract();
        MoneyArithmetic.rounding();
        MoneyArithmetic.compare();
        System.out.println("===== [money] 演示结束 =====");
    }
}
