package lan.chaos.word;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Word 导入导出 Demo 启动类。
 *
 * <p>纯库、零外部中间件：全部场景读写本地临时文件（默认 target/out），
 * 跑测试与 main 都不需要 Redis / DB / 网络。
 */
@SpringBootApplication
public class WordApplication {
    public static void main(String[] args) {
        SpringApplication.run(WordApplication.class, args);
    }
}
