package lan.chaos.serialization;

import lan.chaos.serialization.common.model.User;
import lan.chaos.serialization.jackson.JacksonDemo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacksonDemoTest {

    @Test
    void roundTrip_preservesFields() throws Exception {
        User u = User.sampleUser();
        String json = JacksonDemo.serialize(u);
        User back = JacksonDemo.deserialize(json);

        assertNotNull(json);
        assertEquals(u, back, "JSON 往返后字段应完全一致");
        assertTrue(json.contains("Alice"), "JSON 中应包含关键字段值 Alice（格式无关）");
    }

    @Test
    void toleratesMissingFields() throws Exception {
        User partial = JacksonDemo.deserialize("{\"id\":2,\"name\":\"Bob\"}");
        assertEquals(2L, partial.getId());
        assertEquals("Bob", partial.getName());
        assertNull(partial.getEmail(), "缺失字段应为 null 而非报错");
    }
}
