package lan.chaos.serialization;

import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.jdk.JdkSerializableDemo;
import lan.chaos.serialization.kryo.KryoDemo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KryoDemoTest {

    @Test
    void roundTrip_preservesFields() {
        User u = User.sampleUser();
        byte[] data = KryoDemo.serialize(u);
        User back = KryoDemo.deserialize(data);

        assertArrayEquals(new byte[0], new byte[0]); // sanity
        assertEquals(u, back, "Kryo 二进制往返后字段应完全一致");
    }

    @Test
    void smallerThanJdkNative() throws Exception {
        User u = User.sampleUser();
        int kryoSize = KryoDemo.serialize(u).length;
        int jdkSize = JdkSerializableDemo.serialize(u).length;

        assertTrue(kryoSize < jdkSize,
                "Kryo 二进制应远小于 JDK 原生序列化 (" + kryoSize + " < " + jdkSize + ")");
    }
}
