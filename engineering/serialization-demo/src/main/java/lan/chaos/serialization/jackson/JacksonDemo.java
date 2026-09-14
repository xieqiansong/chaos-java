package lan.chaos.serialization.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.common.util.ByteSizeUtil;

/**
 * Jackson（JSON 文本序列化）演示。
 *
 * <p>WHY：JSON 是「跨语言、人类可读、schema 宽松」的事实标准，RPC/HTTP/配置几乎都靠它。
 * 关键能力：
 * <ul>
 *   <li>双向绑定：{@code writeValueAsString} / {@code readValue}；</li>
 *   <li>健壮性：{@code FAIL_ON_UNKNOWN_PROPERTIES=false} 容忍字段增减（接口演进不崩）；</li>
 *   <li>体积/可读性：{@code INDENT_OUTPUT} 仅美化，生产应关掉；日期默认时间戳，可关。</li>
 * </ul>
 * <p>生产坑：JSON 比二进制体积大、CPU 开销高；大对象/高频链路优先用二进制（见 KryoDemo）。
 */
public class JacksonDemo {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static String serialize(User user) throws Exception {
        return MAPPER.writeValueAsString(user);
    }

    public static User deserialize(String json) throws Exception {
        return MAPPER.readValue(json, User.class);
    }

    public static void demo() throws Exception {
        User u = User.sampleUser();
        String json = serialize(u);
        User back = deserialize(json);

        System.out.println("===== Jackson（JSON 文本）=====");
        System.out.println("序列化结果:\n" + json);
        System.out.println("往返后 name=" + back.getName() + ", roles=" + back.getRoles());
        System.out.println("UTF-8 字节数=" + ByteSizeUtil.utf8Bytes(json));

        // 体现健壮性：缺少字段也能反序列化（接口字段演进）
        User partial = MAPPER.readValue("{\"id\":2,\"name\":\"Bob\"}", User.class);
        System.out.println("容忍未知字段反序列化 => " + partial);
    }
}
