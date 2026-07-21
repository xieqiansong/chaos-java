package lan.chaos.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.common.util.ByteSizeUtil;

/**
 * Kryo（二进制高性能序列化）演示。
 *
 * <p>WHY：Kryo 是 JVM 生态里最快的二进制序列化之一，体积远小于 JSON/JDK，常用于会话复制、缓存、高性能 RPC。
 * <ul>
 *   <li>关键 API：{@code Kryo#writeObject(Output, obj)} / {@code readObject(Input, Class)}；</li>
 *   <li>class 注册：{@code kryo.register(User.class)} 用数字 id 替代写全类名，更小更快；</li>
 *   <li>非线程安全：同一 Kryo 实例不能被多线程并发用，生产用 {@code ThreadLocal<Kryo>} 或对象池。</li>
 * </ul>
 * <p>生产坑：默认要求注册所有类（否则抛错或用 {@code setRegistrationRequired(false)}）；
 * 序列化结果<b>非跨语言</b>、不可人读，且 class 结构变更需谨慎（版本兼容）。
 */
public class KryoDemo {

    private static final Kryo KRYO = new Kryo();
    static {
        // 显式注册主类以获得数字 id（更小更快）；同时关闭强制注册，
        // 让 Date、List 等嵌套类型无需逐个登记也能序列化（演示环境更省心）。
        KRYO.setRegistrationRequired(false);
        KRYO.register(User.class);
    }

    public static byte[] serialize(User user) {
        Output out = new Output(1024, -1);
        KRYO.writeObject(out, user);
        byte[] data = out.toBytes();
        out.close();
        return data;
    }

    public static User deserialize(byte[] bytes) {
        Input in = new Input(bytes);
        User u = KRYO.readObject(in, User.class);
        in.close();
        return u;
    }

    public static void demo() {
        User u = User.sampleUser();
        byte[] data = serialize(u);
        User back = deserialize(data);

        System.out.println("===== Kryo（二进制）=====");
        System.out.println("字节数=" + ByteSizeUtil.bytes(data) + "，往返后 name=" + back.getName());
        System.out.println("提示：Kryo 实例非线程安全，生产请用 ThreadLocal / Pool");
    }
}
