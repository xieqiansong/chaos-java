package lan.chaos.elasticsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch Demo 入口。
 *
 * <h3>两种运行方式</h3>
 * <ol>
 *   <li><b>单元测试（推荐）</b>：{@code mvn test} 借助 Testcontainers 自动拉起 ES 7.17 镜像，
 *       无需手工准备；若本机无 Docker，则测试通过 Assumptions 优雅跳过。</li>
 *   <li><b>Docker 完整体验</b>：{@code docker compose up} 启动 ES，再启动本应用，
 *       可在 Kibana / curl 中交互式把玩索引与查询。</li>
 * </ol>
 *
 * <h3>核心学习点</h3>
 * <ul>
 *   <li>索引管理（Index）：建索引 / 映射 / 删除</li>
 *   <li>文档（Document）：基于 Repository 的 CRUD 与批量写入</li>
 *   <li>搜索（Search）：match / term / range / bool 组合 + 排序分页</li>
 *   <li>聚合（Aggregation）：terms 分桶统计、avg 平均值</li>
 * </ul>
 *
 * @author chaos
 */
@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "lan.chaos.elasticsearch.common.repository")
public class ElasticsearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchApplication.class, args);
    }
}
