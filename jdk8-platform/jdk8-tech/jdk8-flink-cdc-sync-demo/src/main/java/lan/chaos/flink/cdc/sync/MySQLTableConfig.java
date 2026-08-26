package lan.chaos.flink.cdc.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySQLTableConfig implements Serializable {
    private String name;
    private List<String> pk;
    /***
     * 在删除的时候需要提取的字段，默认情况下仅提取pk字段
     */
    @Builder.Default
    private List<String> delResolveColumns = new ArrayList<>();
    /***
     * 在更新的时候需要额外提取的字段，字段默认命名为old.xxx
     */
    @Builder.Default
    private List<String> updateResolveColumns = new ArrayList<>();
}
