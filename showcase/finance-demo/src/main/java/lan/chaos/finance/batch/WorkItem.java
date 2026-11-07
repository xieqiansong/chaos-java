package lan.chaos.finance.batch;

import lombok.Data;

/** 批处理工作项：{@code id} 是幂等键，{@code shard} 是分片号。 */
@Data
public class WorkItem {
    private String id;
    private int shard;
    private String payload;
}
