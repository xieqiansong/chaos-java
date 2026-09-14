# jdk8-seckill-demo

## 一句话定位
一个支持万人并发抢购的**秒杀系统** Demo，覆盖「防超卖 / 防超买 / 限流 / 异步下单」四大核心难点。
技术主线：Redis 分桶库存 + Lua 原子扣减 + Redisson 分布式锁 + Kafka 异步下单 + 令牌桶限流。

> 原仓库 `chaos-java-example/demo-seckill` 的迁移版，包名 `lan.chaos.demo.seckill` 与依赖保持原样。

## 技术栈
- Spring Boot 2.7.18 (Web / JPA / Validation)
- Redis（库存分桶、限流计数）+ Redisson（分布式锁）
- PostgreSQL（商品 / 订单持久化，JPA `ddl-auto: update` 自动建表）
- Kafka（秒杀成功后异步落单，削峰填谷）
- Lombok + Caffeine（本地缓存）

入口类：`lan.chaos.demo.seckill.SeckillApplication`，默认端口 `8081`。

## 快速开始

### 1. 启动依赖组件（端口 / 密码已对齐 application.yml）
```bash
cd jdk8-seckill-demo
docker compose up -d        # PostgreSQL:30101 / Redis:30102 / Kafka:30103
```

### 2. 启动应用
```bash
# 方式 A：IDE 直接运行 SeckillApplication
# 方式 B：Maven
mvn -pl jdk8-seckill-demo -am spring-boot:run
```

### 3. 验证（核心流程）
```bash
# 3.1 创建秒杀商品（返回商品 id）
curl -X POST http://localhost:8081/api/admin/product \
  -H "Content-Type: application/json" \
  -d '{"productName":"iPhone 16 Pro","totalStock":1000,"bucketCount":10,"price":8999.00,"startTime":"2027-01-01T10:00:00","endTime":"2027-01-01T11:00:00"}'

# 3.2 启动秒杀活动（把上一步返回的 id 填进去，例如 1）
curl -X POST http://localhost:8081/api/admin/product/1/activate

# 3.3 查询库存
curl http://localhost:8081/api/admin/product/1/stock

# 3.4 发起秒杀（会返回订单 token）
curl -X POST http://localhost:8081/api/seckill/1 \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-001","quantity":1}'

# 3.5 用返回的 token 查订单
curl http://localhost:8081/api/orders/<token>
```
> 重复 3.4 多次即可观察库存递减、超卖被拦截（Lua 原子扣减保证）。

## 场景一览（接口）
| 方法 & 路径 | 说明 |
|------|------|
| `POST /api/admin/product` | 创建秒杀商品并初始化 Redis 分桶库存 |
| `POST /api/admin/product/{id}/activate` | 启动秒杀活动 |
| `GET  /api/admin/product/{id}/stock` | 查询剩余库存 / 是否售罄 |
| `POST /api/seckill/{productId}` | 秒杀入口（令牌桶限流 → 校验 → Lua 扣库存 → Kafka 异步下单） |
| `GET  /api/orders/{token}` | 凭令牌查询异步订单结果 |

## 设计要点（为什么这么拆）
- **分桶库存**：`InventoryService.initStock` 把总库存均摊到 N 个 bucket，秒杀时随机选桶扣减，把单 key 热点打散到多 key，避免 Redis 单分片热点。
- **Lua 原子扣减防超卖**：`lua/stock_bucket.lua` 在 Redis 端原子完成「判断售罄 → 扣减」，杜绝「查-扣」之间的并发竞态。
- **令牌桶限流**：`RateLimitService` 基于 `seckill.rate-limit.default-qps` 在入口拦掉超额流量，保护下游。
- **Kafka 异步下单**：秒杀成功仅做库存扣减 + 发消息，真正创建订单由消费者异步完成，削峰填谷。
- **库存回写**：`InventorySyncTask` 定时把 Redis 库存同步回 PostgreSQL，保证最终一致。

## 进阶方向（生产化考量）
- 库存预热与隔离：分桶策略按热点商品动态调整。
- 防刷 / 防黄牛：用户维度限流、验证码、风控。
- 订单超时回滚：未支付订单定时释放库存。
- 灰度与压测：配合 `压力测试.md` 中记录的压测方案做容量规划。
