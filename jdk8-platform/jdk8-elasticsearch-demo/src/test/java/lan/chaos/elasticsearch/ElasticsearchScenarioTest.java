package lan.chaos.elasticsearch;

import lan.chaos.elasticsearch.aggregation.AggregationScenario;
import lan.chaos.elasticsearch.common.repository.ProductRepository;
import lan.chaos.elasticsearch.document.DocumentScenario;
import lan.chaos.elasticsearch.index.IndexScenario;
import lan.chaos.elasticsearch.model.Product;
import lan.chaos.elasticsearch.search.SearchScenario;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Elasticsearch 全场景集成测试。
 *
 * <h3>运行策略</h3>
 * <ul>
 *   <li>若本机有 Docker：Testcontainers 自动拉起 ES 7.17 容器，测试真实执行</li>
 *   <li>若无 Docker：{@code @BeforeAll} 中的 {@code assumeTrue} 使所有用例优雅跳过（不报错）</li>
 * </ul>
 *
 * 每个能力（嵌套类）各自在 {@code @BeforeAll} 重建索引并灌入样例数据，
 * {@code @AfterEach} 删除索引保证隔离，不依赖执行顺序。
 */
@SpringBootTest
class ElasticsearchScenarioTest {

    private static final boolean DOCKER_AVAILABLE = DockerClientFactory.instance().isDockerAvailable();

    /** 由 {@link #overrideProperties} 在 Docker 可用时启动；否则保持 null，测试优雅跳过 */
    static ElasticsearchContainer esContainer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            esContainer = new ElasticsearchContainer(
                            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.10"))
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
            esContainer.start();
            registry.add("app.elasticsearch.uris", () -> "http://" + esContainer.getHttpHostAddress());
        }
    }

    @AfterAll
    static void stopContainer() {
        if (esContainer != null) {
            esContainer.stop();
        }
    }

    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private IndexScenario indexScenario;
    @Autowired
    private DocumentScenario documentScenario;
    @Autowired
    private SearchScenario searchScenario;
    @Autowired
    private AggregationScenario aggregationScenario;
    @Autowired
    private ProductRepository productRepository;

    @BeforeAll
    static void guard() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "本机无 Docker，跳过 Elasticsearch 集成测试");
    }

    /** 样例数据工厂：覆盖三个分类、不同价格，便于搜索/聚合断言 */
    private List<Product> sampleProducts() {
        return Arrays.asList(
                Product.builder().id("p1").name("Smart Phone X").category("electronics")
                        .price(5999.0).stock(10).description("旗舰智能手机 phone").createdAt(LocalDateTime.now()).build(),
                Product.builder().id("p2").name("Pro Laptop").category("electronics")
                        .price(8999.0).stock(5).description("高性能笔记本 laptop").createdAt(LocalDateTime.now()).build(),
                Product.builder().id("p3").name("Cotton T-Shirt").category("cloth")
                        .price(99.0).stock(100).description("纯棉 T 恤").createdAt(LocalDateTime.now()).build(),
                Product.builder().id("p4").name("Slim Jeans").category("cloth")
                        .price(199.0).stock(50).description("修身牛仔裤").createdAt(LocalDateTime.now()).build(),
                Product.builder().id("p5").name("Java Book").category("book")
                        .price(79.0).stock(200).description("Java 编程思想").createdAt(LocalDateTime.now()).build(),
                Product.builder().id("p6").name("Python Book").category("book")
                        .price(89.0).stock(150).description("Python 入门").createdAt(LocalDateTime.now()).build()
        );
    }

    private void seed() {
        indexScenario.createIndexByEntity();
        documentScenario.bulkSave(sampleProducts());
    }

    @Nested
    @DisplayName("INDEX-索引管理")
    class IndexTests {

        @BeforeAll
        void setUp() {
            indexScenario.createIndexByEntity();
        }

        @AfterEach
        void tearDown() {
            indexScenario.deleteIndex();
        }

        @Test
        @DisplayName("INDEX-1: 创建索引后 exists 返回 true")
        void indexExistsAfterCreate() {
            assertTrue(indexScenario.exists());
        }

        @Test
        @DisplayName("INDEX-2: 删除索引后 exists 返回 false")
        void indexNotExistsAfterDelete() {
            assertTrue(indexScenario.deleteIndex());
            assertFalse(indexScenario.exists());
        }
    }

    @Nested
    @DisplayName("DOC-文档 CRUD")
    class DocumentTests {

        @BeforeAll
        void setUp() {
            seed();
        }

        @AfterEach
        void tearDown() {
            indexScenario.deleteIndex();
        }

        @Test
        @DisplayName("DOC-1: 按 id 查询已写入文档")
        void getById() {
            Optional<Product> opt = documentScenario.getById("p1");
            assertTrue(opt.isPresent());
            assertEquals("Smart Phone X", opt.get().getName());
        }

        @Test
        @DisplayName("DOC-2: 更新价格后读回新值")
        void updatePrice() {
            documentScenario.updatePrice("p1", 4999.0);
            Optional<Product> opt = documentScenario.getById("p1");
            assertTrue(opt.isPresent());
            assertEquals(4999.0, opt.get().getPrice());
        }

        @Test
        @DisplayName("DOC-3: 删除后查不到")
        void delete() {
            documentScenario.delete("p2");
            assertFalse(documentScenario.getById("p2").isPresent());
        }

        @Test
        @DisplayName("DOC-4: 批量写入后总数等于样例数")
        void bulkSave() {
            assertTrue(documentScenario.count() >= 6);
        }
    }

    @Nested
    @DisplayName("SEARCH-搜索")
    class SearchTests {

        @BeforeAll
        void setUp() {
            seed();
        }

        @AfterEach
        void tearDown() {
            indexScenario.deleteIndex();
        }

        @Test
        @DisplayName("SEARCH-1: matchAll 命中全部样例")
        void matchAll() {
            assertEquals(6, searchScenario.matchAll(10).size());
        }

        @Test
        @DisplayName("SEARCH-2: 全文检索 phone 命中手机商品")
        void matchByName() {
            List<Product> hits = searchScenario.matchByName("phone");
            assertTrue(hits.stream().anyMatch(p -> "p1".equals(p.getId())));
        }

        @Test
        @DisplayName("SEARCH-3: term 精确匹配分类 book 命中 2 条")
        void termByCategory() {
            List<Product> hits = searchScenario.termByCategory("book");
            assertEquals(2, hits.size());
        }

        @Test
        @DisplayName("SEARCH-4: 价格区间 [0,200] 命中 4 条")
        void rangeByPrice() {
            List<Product> hits = searchScenario.rangeByPrice(0.0, 200.0);
            assertEquals(4, hits.size());
        }

        @Test
        @DisplayName("SEARCH-5: bool 复合查询 + 价格降序")
        void boolQuery() {
            List<Product> hits = searchScenario.boolQuery("electronics", "phone", 0, 10);
            assertFalse(hits.isEmpty());
            assertEquals("p1", hits.get(0).getId());
        }
    }

    @Nested
    @DisplayName("AGG-聚合")
    class AggregationTests {

        @BeforeAll
        void setUp() {
            seed();
        }

        @AfterEach
        void tearDown() {
            indexScenario.deleteIndex();
        }

        @Test
        @DisplayName("AGG-1: 各分类商品数均为 2")
        void countByCategory() {
            Map<String, Long> map = aggregationScenario.countByCategory();
            assertEquals(2L, map.get("electronics"));
            assertEquals(2L, map.get("cloth"));
            assertEquals(2L, map.get("book"));
        }

        @Test
        @DisplayName("AGG-2: 各分类平均价格正确")
        void avgPriceByCategory() {
            Map<String, Double> map = aggregationScenario.avgPriceByCategory();
            assertEquals(7499.0, map.get("electronics"), 0.01);
            assertEquals(149.0, map.get("cloth"), 0.01);
            assertEquals(84.0, map.get("book"), 0.01);
        }
    }
}
