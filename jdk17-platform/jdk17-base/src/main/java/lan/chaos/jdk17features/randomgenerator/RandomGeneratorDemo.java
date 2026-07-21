package lan.chaos.jdk17features.randomgenerator;

import java.util.random.RandomGenerator;

/**
 * RandomGenerator 工厂（JEP 356，JDK17）：统一的随机数生成器接口 + 多种算法，替代 {@code new Random()} 单一实现。
 *
 * <p>WHY：旧 API 只有 {@code Random}/{@code SecureRandom}，算法不可选、接口不统一。JDK17 引入 {@code RandomGenerator} 接口：
 * <ul>
 *   <li>{@code RandomGenerator.getDefault()} 取默认算法（随版本演进可变化）；</li>
 *   <li>{@code RandomGenerator.of("Xoshiro256PlusPlus")} 按名取具体算法；</li>
 *   <li>统一提供 {@code ints/longs/doubles} 流方法，便于批量采样。</li>
 * </ul>
 */
public class RandomGeneratorDemo {

    public static void run() {
        RandomGenerator def = RandomGenerator.getDefault();
        System.out.println("默认算法: " + def.getClass().getSimpleName());
        System.out.println("默认 nextInt(100): " + def.nextInt(100));

        RandomGenerator xoshiro = RandomGenerator.of("Xoshiro256PlusPlus");
        System.out.println("Xoshiro256PlusPlus nextInt(100): " + xoshiro.nextInt(100));

        long count = def.ints(5, 0, 10).count();
        System.out.println("ints(5,0,10) 流元素数: " + count);
    }
}
