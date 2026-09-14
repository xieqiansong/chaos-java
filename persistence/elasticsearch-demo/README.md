# jdk8-elasticsearch-demo

> Elasticsearch 搜索引擎学习 Demo：索引管理 / 文档 CRUD / 搜索 / 聚合，含 Testcontainers 集成测试与单机环境。

## 技术栈

| 分层 | 选型 | 说明 |
|------|------|------|
| 搜索引擎 | Elasticsearch 7.17.10 | 与 JDK8 / Spring Boot 2.7 匹配的版本组合 |
| 数据访问 | Spring Data Elasticsearch 4.4 | `ElasticsearchRestTemplate` + `ElasticsearchRepository` 双轨 |
| 客户端 | RestHighLevelClient | Spring Boot 2.7 时代官方客户端 |
| 测试 | Testcontainers 1.17 | 自动拉起 ES 容器，Docker 不可用时优雅跳过 |

## 快速开始

```bash
# 方式一：单元测试（推荐，无需手动准备）
# Docker 可用时自动起 ES 7.17 容器并真实执行；无 Docker 时用例优雅跳过
mvn -pl jdk8-elasticsearch-demo -am test

# 方式二：Docker 完整体验
docker compose -f jdk8-elasticsearch-demo/docker-compose.yml up -d
# 验证 ES 已就绪
curl http://localhost:9200/_cluster/health
mvn -pl jdk8-elasticsearch-demo -am spring-boot:run
```

启动后可用 curl 直接把玩（索引已存在时）：

```bash
# 查看索引
curl http://localhost:9200/_cat/indices?v
# 全文检索
curl -X POST "http://localhost:9200/product/_search" -H 'Content-Type: application/json' \
  -d '{"query":{"multi_match":{"query":"phone","fields":["name","description"]}}}'
```

## 场景一览

| # | 能力 | 场景文件 | 一句话 |
|---|------|---------|--------|
| INDEX-1 | 索引创建 | `index/IndexScenario#createIndexByEntity` | 基于实体注解建索引 + 写入 mapping |
| INDEX-2 | 索引删除 | `index/IndexScenario#deleteIndex` | 删除后 exists 返回 false |
| DOC-1 | 按 id 查询 | `document/DocumentScenario#getById` | 写入后按 id 立即查回 |
| DOC-2 | 更新字段 | `document/DocumentScenario#updatePrice` | upsert 语义，改价后读回新值 |
| DOC-3 | 删除文档 | `document/DocumentScenario#delete` | 删除后查不到 |
| DOC-4 | 批量写入 | `document/DocumentScenario#bulkSave` | saveAll 走 _bulk，高效灌数据 |
| SEARCH-1 | matchAll | `search/SearchScenario#matchAll` | 命中全部文档 |
| SEARCH-2 | 全文检索 | `search/SearchScenario#matchByName` | multiMatch 对 text 字段分词匹配 |
| SEARCH-3 | 精确匹配 | `search/SearchScenario#termByCategory` | term 对 keyword 字段精确匹配 |
| SEARCH-4 | 范围查询 | `search/SearchScenario#rangeByPrice` | 数值区间 [min,max] |
| SEARCH-5 | 复合查询 | `search/SearchScenario#boolQuery` | must + should + 价格降序分页 |
| AGG-1 | 分桶统计 | `aggregation/AggregationScenario#countByCategory` | terms 聚合按分类计数 |
| AGG-2 | 平均值 | `aggregation/AggregationScenario#avgPriceByCategory` | terms 嵌套 avg 子聚合 |

## 场景详解

### 索引管理（Index）

ES 是 schema-less 但有 mapping：字段类型一旦写入便难以修改。生产环境应显式建索引（settings + mappings），而非依赖首次写入的 auto-mapping（可能因首条数据误判类型）。

`IndexOperations` 既支持基于实体类的"约定式"建索引（`create()` + `putMapping(Product.class)`），也支持完全自定义的 mapping JSON。本 Demo 用前者，代码即文档。

### 文档（Document）

基于 `ElasticsearchRepository` 的声明式 CRUD：

- `save`：未指定 id 时 ES 自动生成；指定 id 为 upsert
- `saveAll`：批量写入，内部走 `_bulk`，比逐条 save 高效
- **近实时陷阱**：默认写入后需 `refresh` 才能被搜索立即看到，演示中显式 `refresh()` 以便测试断言。生产环境不要每次写入都 refresh（有性能损耗），应依赖默认近实时刷新周期（1s）

### 搜索（Search）

| 查询 | 字段类型 | 说明 |
|------|---------|------|
| `matchQuery` / `multiMatchQuery` | text | 先分词再匹配，适合商品名/描述全文检索 |
| `termQuery` | keyword | 不分词精确匹配，适合分类/状态枚举 |
| `rangeQuery` | numeric/date | 价格区间、时间区间 |
| `boolQuery` | 组合 | must（与）/ should（或，配 minimumShouldMatch）/ filter / mustNot |

### 聚合（Aggregation）

类似 SQL 的 `GROUP BY`，典型场景是各分类商品数、平均价格。

- terms 聚合只能作用在 **keyword** 字段（text 需加 `.keyword` 子字段）
- 聚合与搜索共用查询：可先 query 过滤，再对过滤结果聚合
- 只看聚合结果时 `setMaxResults(0)` 不返回明细，节省开销

返回结构：`SearchHits` → `getAggregations()`（Spring Data 的 `AggregationsContainer`）→ 底层 ES `Aggregations` → `terms` / `avg` 等。

## 测试说明

测试通过 Testcontainers 在 Docker 可用时自动拉起 ES 7.17 容器并执行真实读写；**无 Docker 时由 `@BeforeAll` 的 `Assumptions.assumeTrue` 优雅跳过**，不影响 CI 通过。

每个能力（嵌套测试类）各自在 `@BeforeAll` 重建索引并灌入样例数据，`@AfterEach` 删除索引，保证用例隔离、不依赖执行顺序。

## 进阶方向

- **分词器**：IK 中文分词器、自定义 analyzer，提升中文检索召回
- **高亮**：search 结果对匹配关键词高亮显示
- **索引别名 + 重建**：通过 alias 实现零停机索引重建（reindex）
- **集群与分片**：多节点、分片/副本调优、冷热架构
- **Spring Data 新版**：ES 8.x + 新 Java API Client（co.elastic.clients）+ Spring Boot 3（需 JDK17+）
- **生产化**：索引模板（index template）、ILM 生命周期管理、批量写入调优

## 设计要点

- **为什么核心场景用 `ElasticsearchRestTemplate` 而非只用 Repository**：Repository 适合简单 CRUD，但搜索/聚合/索引管理需要更底层的 `NativeSearchQuery`，两者并存覆盖更全
- **text vs keyword 选择**：全文检索用 text，精确匹配与聚合用 keyword——这是 ES 建模最易踩的坑
- **近实时与 refresh**：演示中为断言强制 refresh，生产环境应避免高频 refresh
- **测试隔离**：ES 索引是全局状态，每个能力用例自建自毁，避免交叉污染
