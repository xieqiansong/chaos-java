package lan.chaos.jdk25features.instancemain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InstanceMainDemoTest {

    @Test
    void instanceMainRuns() {
        InstanceMainDemo demo = new InstanceMainDemo();
        assertDoesNotThrow(demo::main);
    }
}
