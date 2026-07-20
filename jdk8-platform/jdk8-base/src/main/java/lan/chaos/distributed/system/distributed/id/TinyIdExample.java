package lan.chaos.distributed.system.distributed.id;

import com.xiaoju.uemc.tinyid.client.utils.TinyId;

import java.util.List;

public class TinyIdExample {
    public static void main(String[] args) {
        Long id = TinyId.nextId("test");
        List<Long> ids = TinyId.nextId("test", 10);
    }
}
