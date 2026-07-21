package lan.chaos.jdk25features.moduleimport;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleImportDemoTest {

    @Test
    void javaBaseTypesUsable() {
        // 验证 java.base 导出的公共类型可直接使用（无需逐条 import）
        List<Integer> nums = List.of(1, 2, 3);
        Map<String, Integer> m = Map.of("a", 1);
        assertEquals(3, nums.size());
        assertEquals(1, m.get("a"));

        // 运行 demo 不抛异常即通过
        ModuleImportDemo.run();
    }
}
