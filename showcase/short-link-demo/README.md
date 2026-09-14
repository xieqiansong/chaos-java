# jdk8-short-link-demo

## 一句话定位
一个高并发、低延迟的**短链生成与跳转**服务 Demo，覆盖「短码生成 / 缓存 / 防缓存穿透 / 302 跳转」核心链路。
技术主线：Snowflake 发号 + Base62 编码短码 + Redisson 布隆过滤器 + Redis 缓存 + PostgreSQL 持久化。

> 原仓库 `chaos-java-example/demo-short-link` 的迁移版，包名 `lan.chaos.demo.shortlink` 与依赖保持原样。

## 技术栈
- Spring Boot 2.7.18 (Web / JPA / Validation)
- Redis（短链缓存）+ Redisson（布隆过滤器）
- PostgreSQL（短链持久化，JPA `ddl-auto: update` 自动建表）
- Lombok + Caffeine（本地缓存）

入口类：`lan.chaos.demo.shortlink.ShortLinkApplication`，默认端口 `8080`。

## 快速开始

### 1. 启动依赖组件（端口 / 密码已对齐 application.yml）
```bash
cd jdk8-short-link-demo
docker compose up -d        # PostgreSQL:30101 / Redis:30102
```

### 2. 启动应用
```bash
# 方式 A：IDE 直接运行 ShortLinkApplication
# 方式 B：Maven
mvn -pl jdk8-short-link-demo -am spring-boot:run
```

### 3. 验证
```bash
# 3.1 健康检查
curl http://localhost:8080/api/health

# 3.2 生成短链（返回 shortKey 与完整短链地址）
curl -X POST http://localhost:8080/api/short-link \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/very-long-url","expireTime":"2027-12-31 23:59:59"}'

# 3.3 跳转（302 重定向到原地址，可用 -i 看 Location）
curl -i http://localhost:8080/<shortKey>
```

## 场景一览（接口）
| 方法 & 路径 | 说明 |
|------|------|
| `POST /api/short-link` | 生成短链：Snowflake 发号 → Base62 编码 → 写 PG/Redis/布隆过滤器 |
| `GET  /{shortKey}` | 短链跳转：布隆过滤器拦截非法 key → Redis 取原 URL → 302 重定向 |
| `GET  /api/health` | 健康检查 |

## 设计要点（为什么这么拆）
- **Snowflake + Base62**：`SnowflakeIdGenerator` 生成趋势递增的唯一 ID，`Base62Util.encode` 转为无符号短串（如 `a3d9Kj`），URL 友好且不可轻易遍历。
- **布隆过滤器防缓存穿透**：`BloomFilterConfig` 初始化 `short-key-bloom`，跳转前先 `contains` 判断，非法 key 直接拒绝，避免打到 Redis/DB。
- **Redis 缓存 + PG 持久化**：热点短链走 Redis；`short:` 为 key。PG 做最终存储，`short_key` 建唯一索引。
- **302 跳转**：返回 `HttpStatus.FOUND` + `Location` 头，对 SEO 友好且可统计点击。

## 进阶方向（生产化考量）
- 短码冲突与重试：Base62 碰撞时换发号或加盐。
- 缓存一致性：短链更新 / 过期时同步失效 Redis + 布隆过滤器重建策略。
- 高可用发号：Snowflake 的 workerId 需结合部署环境（如 ZooKeeper/Redis）分配，避免时钟回拨。
- 访问统计：跳转时异步埋点（点击量、来源、地域）。
