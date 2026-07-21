package lan.chaos.jdk25features.moduleimport;

import module java.base;

/**
 * 模块导入声明 Module Import Declarations（JEP 476，JDK25 定稿）：一行 {@code import module M;} 即可导入该模块导出的所有包的公共类型。
 *
 * <p>WHY：以前要用 {@code java.util.List}/{@code java.util.Map}/{@code java.io.*} 等得逐个 import；
 * 模块导入声明特别适合教学/脚本场景，把整个 {@code java.base} 的导出类型一次性拉进作用域。
 * 注意：它导入的是"公共 API 类型"，不影响可访问性，仅为省去一条条 import。
 */
public class ModuleImportDemo {

    public static void run() {
        // 无需单独 import java.util.List / java.util.Map —— import module java.base 已包含
        List<String> list = List.of("a", "b", "c");
        Map<String, Integer> map = Map.of("k", 1);
        System.out.println("模块导入后直接使用: " + list + " / " + map);
        System.out.println("可直接用 java.base 任意导出类型，如 Math.PI=" + Math.PI);
    }
}
