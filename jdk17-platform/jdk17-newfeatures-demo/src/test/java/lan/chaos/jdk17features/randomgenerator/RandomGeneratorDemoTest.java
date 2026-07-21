package lan.chaos.jdk17features.randomgenerator;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGeneratorDemoTest {

    @Test
    void defaultGenerator() {
        RandomGenerator gen = RandomGenerator.getDefault();
        assertNotNull(gen);
        int v = gen.nextInt(10);
        assertTrue(v >= 0 && v < 10);
    }

    @Test
    void namedAlgorithm() {
        RandomGenerator xoshiro = RandomGenerator.of("Xoshiro256PlusPlus");
        assertNotNull(xoshiro);
        int v = xoshiro.nextInt(100);
        assertTrue(v >= 0 && v < 100);
    }
}
