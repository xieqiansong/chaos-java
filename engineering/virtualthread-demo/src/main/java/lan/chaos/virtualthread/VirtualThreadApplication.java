package lan.chaos.virtualthread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 虚拟线程技术点 Demo 启动类。
 * 根包 {@code lan.chaos.virtualthread} 遵循 `chaos-java/AGENTS.md` 根包约定（无 demo 中间层）。
 * 技术点本质：IO 阻塞不占 OS 线程（阻塞即卸载让出载体线程），
 * 但 synchronized 临界区内阻塞会 pin 住载体线程，使虚拟线程退化为平台线程的并发能力。
 */
@SpringBootApplication
public class VirtualThreadApplication {

    public static void main(String[] args) {
        SpringApplication.run(VirtualThreadApplication.class, args);
    }
}
