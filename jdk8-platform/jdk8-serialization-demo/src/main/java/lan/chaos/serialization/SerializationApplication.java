package lan.chaos.serialization;

import lan.chaos.serialization.jackson.JacksonDemo;
import lan.chaos.serialization.jdk.JdkSerializableDemo;
import lan.chaos.serialization.kryo.KryoDemo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 序列化技术点入口：三种方式分节打印「输入 → 输出（体积 + 往返一致性）」。
 */
@SpringBootApplication
public class SerializationApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SerializationApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        JacksonDemo.demo();
        System.out.println();
        KryoDemo.demo();
        System.out.println();
        JdkSerializableDemo.demo();
    }
}
