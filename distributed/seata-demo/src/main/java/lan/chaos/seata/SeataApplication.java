package lan.chaos.seata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Seata 分布式事务 Demo 启动入口。
 *
 * <h3>两种运行方式</h3>
 * <ol>
 *   <li><b>mvn test</b>：H2 内存数据库 + seata.enabled=false，验证业务逻辑与本地事务</li>
 *   <li><b>docker compose + java -jar</b>：连接真实 Seata Server + MySQL，体验完整分布式事务回滚</li>
 * </ol>
 *
 * <h3>核心学习点</h3>
 * <ul>
 *   <li>AT 模式：Seata 自动管理 undo_log，@GlobalTransactional 协调多数据源</li>
 *   <li>TCC 模式：手动 Try/Confirm/Cancel，更精细的资源控制</li>
 * </ul>
 *
 * @author chaos
 */
@SpringBootApplication
public class SeataApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeataApplication.class, args);
    }
}
