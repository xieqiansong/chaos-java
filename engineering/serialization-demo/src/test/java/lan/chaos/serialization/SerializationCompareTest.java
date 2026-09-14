package lan.chaos.serialization;

import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.common.util.ByteSizeUtil;
import lan.chaos.serialization.jackson.JacksonDemo;
import lan.chaos.serialization.jdk.JdkSerializableDemo;
import lan.chaos.serialization.kryo.KryoDemo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 横向对比：证明 JDK 原生序列化体积最大，二进制(Kryo) < 文本(JSON) < JDK 原生。
 */
class SerializationCompareTest {

    @Test
    void jdkNative_isLargest() throws Exception {
        User u = User.sampleUser();
        int jdk = JdkSerializableDemo.serialize(u).length;
        int kryo = KryoDemo.serialize(u).length;
        int jackson = ByteSizeUtil.utf8Bytes(JacksonDemo.serialize(u));

        assertTrue(jdk > kryo, "JDK>Kryo: " + jdk + " vs " + kryo);
        assertTrue(jdk > jackson, "JDK>Jackson: " + jdk + " vs " + jackson);
        System.out.println("体积对比(字节) JDK=" + jdk + " | Jackson=" + jackson + " | Kryo=" + kryo);
    }
}
