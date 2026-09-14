package lan.chaos.serialization.jdk;

import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.common.util.ByteSizeUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * JDK 原生 {@link java.io.Serializable} 序列化演示。
 *
 * <p>WHY：零依赖、开箱即用，是很多框架的兜底序列化。但代价明显：
 * <ul>
 *   <li>体积最大、速度最慢（自带大量类元数据/校验）；</li>
 *   <li>强耦合类结构：类变更需靠 {@code serialVersionUID} 控制兼容，否则 {@code InvalidClassException}；</li>
 *   <li>安全黑洞：反序列化不可信字节流可触发 gadget chain 远程代码执行（如 Apache Commons Collections 历史漏洞），
 *       生产应尽量避免反序列化不可信输入，或加 {@code ObjectInputFilter} 白名单。</li>
 * </ul>
 */
public class JdkSerializableDemo {

    public static byte[] serialize(User user) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(user);
        }
        return bos.toByteArray();
    }

    public static User deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (User) ois.readObject();
        }
    }

    public static void demo() throws Exception {
        User u = User.sampleUser();
        byte[] data = serialize(u);
        User back = deserialize(data);

        System.out.println("===== JDK 原生 Serializable =====");
        System.out.println("字节数=" + ByteSizeUtil.bytes(data) + "，往返后 name=" + back.getName());
    }
}
