package lan.chaos.distributed.system.distributed.id;

import com.tencent.devops.leaf.service.SegmentService;
import org.springframework.beans.factory.annotation.Autowired;

public class LeafSegmentExample {
    @Autowired
    private SegmentService segmentService;

    public void generateId() {
        // 生成ID，参数为业务标识(biz_tag)[reference:21]
        long id = segmentService.getId("test1").getId();
        System.out.println("Generated ID: " + id);
    }
}