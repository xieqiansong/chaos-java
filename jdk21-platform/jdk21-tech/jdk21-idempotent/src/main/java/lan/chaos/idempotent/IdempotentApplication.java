package lan.chaos.idempotent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 接口幂等 Demo 启动类。
 * 根包 {@code lan.chaos.idempotent} 遵循 `chaos-java/AGENTS.md` 根包约定（无 demo 中间层）。
 * 技术点本质：在 at-least-once 投递语义下，用「去重键 + 首检判定」保证重复请求/消息不造成重复副作用。
 */
@SpringBootApplication
public class IdempotentApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotentApplication.class, args);
    }
}
