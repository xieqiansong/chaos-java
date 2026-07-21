package lan.chaos.jdk8features.defaultmethod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultMethodDemoTest {

    @Test
    void defaultAndStaticAndOverride() {
        Calculator calc = Calculator.create();
        assertEquals(7, calc.add(3, 4));
        assertEquals(12, calc.multiply(3, 4)); // 享用接口默认实现

        Calculator overridden = new Calculator() {
            @Override
            public int add(int a, int b) {
                return a + b;
            }

            @Override
            public int multiply(int a, int b) {
                return (a * b) * 2; // 重写默认实现
            }
        };
        assertEquals(24, overridden.multiply(3, 4));
    }
}
