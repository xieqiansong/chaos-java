package lan.chaos.distributed.system.distributed.id;

import java.util.UUID;

public class UUIDExample {
    public static void main(String[] args) {
        // 生成一个标准的UUID，包含"-"符号
        String uuid = UUID.randomUUID().toString();
        System.out.println("标准UUID: " + uuid); 
        // 输出示例: 550e8400-e29b-41d4-a716-446655440000[reference:1]

        // 生成不带"-"符号的UUID (更紧凑，常用于数据库主键)
        String compactUuid = UUID.randomUUID().toString().replace("-", "");
        System.out.println("紧凑UUID: " + compactUuid);
    }
}