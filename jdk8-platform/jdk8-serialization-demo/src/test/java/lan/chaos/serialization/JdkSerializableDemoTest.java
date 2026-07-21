package lan.chaos.serialization;

import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.jdk.JdkSerializableDemo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JdkSerializableDemoTest {

    @Test
    void roundTrip_preservesFields() throws Exception {
        User u = User.sampleUser();
        byte[] data = JdkSerializableDemo.serialize(u);
        User back = JdkSerializableDemo.deserialize(data);

        assertEquals(u, back, "JDK 原生序列化往返应完全一致");
        assertTrue(data.length > 0);
    }
}
