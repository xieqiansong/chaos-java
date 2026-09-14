# HMAC 无状态签名鉴权 Demo（jdk8-hmac-auth-demo）

> 纯 JDK 8、**零运行时依赖**。演示把「集中存储 Token + 每请求读 Redis 校验」演进为「HMAC 无状态签名 + 本地验签」的完整机制。
> 去隐私说明：业务实体已泛化为 device / 上报请求，不含任何业务与公司信息。

## 定位（背景演进）

```
早期链路（瓶颈点）：
  ① 开机认证：AES-CBC 解密 + 固定 Token 校验 → 通过后签发 Token 存 Redis
  ② 后续上报：请求带 Token → 网关每请求去 Redis 读 Token 校验   ← 每请求 1 次网络往返，吞吐卡在这
后期演进：
  ③ HMAC 改造：认证与上报统一为 HMAC 无状态签名 → 网关本地验签，彻底去掉 ② 的 Redis 读
```

Demo 覆盖的技术问题（对应必会题 Q1-Q3）：
- **Q1 QPS 提升（1000+ → 10000+）**：场景 D 实测「每请求 1 次 Redis 读」与「本地验签 0 往返」的吞吐/耗时差距。
- **Q2 HMAC 设计 + 密钥轮换**：签名串规范、时间戳时间窗、防重放、双密钥过渡轮换。
- **Q3 HMAC vs Token/AES 选型演进**：无状态签名 vs 集中存储 Token/AES 的动机与取舍（本节 + 防重放分层）。

## 功能（四场景）

| 场景 | 内容 | 对应问题 |
|------|------|---------|
| A 签名 / 验签 | 签名串规范（method+path+ts+nonce+bodyDigest）+ HMAC-SHA256 + 常量时间比较；篡改/错误密钥被拒 | Q2 |
| B 防重放 | 时间戳时间窗（0 存储）+ 滑动窗口频率限制 + 业务幂等键去重（写侧兜底） | Q2 |
| C 密钥轮换 | 双密钥过渡：新钥立即签发、旧钥宽限期内仍可验签、宽限期后丢弃 | Q2 |
| D 吞吐对比 | 本地验签（0 往返）vs Token+Redis 读校验（模拟 1ms 往返），输出 QPS / avg / p99 | Q1 |

## 防重放为什么不上 Redis（容量账）

引入 HMAC 就是为了去掉每请求的 Redis 读；若 nonce 防重放又去 Redis `SETNX`，等于把「读 Token」换成「读 nonce」，**瓶颈原地搬家**。

| 档位 | 机制 | 网络往返 | 适用场景 |
|------|------|---------|---------|
| 默认（高频上报） | 时间戳时间窗 + **业务幂等兜底**（deviceId+batchNo 唯一键，写侧去重） | 0 | 上报主链路 |
| 加强（低频敏感操作） | nonce + Redis SETNX | 1 | 配置/指令下发（分钟级频率，一次往返值得） |
| 不做 | 每设备内存 nonce / 全量 nonce 缓存 | 0 | 跨节点与空间账不支持 |

**内存账（1200 万设备、设备 ID 10~15 字母数字）**：
- `String` 设备ID(≈72B) + `String` nonce(≈112B) + HashMap.Node(≈32B) ≈ 216B/条 → **≈2.6 GB**
- 即使优化为 `byte[]` ID + 时间戳 ≈ 88B/条 → 仍 **≈1.06 GB**（未计 GC/过期扫描/扩容迁移）

**跨节点账**：设备上报源 IP 是运营商 NAT 出口，一个地市几十万台可能共用少量出口 IP——IP 吸附必炸单点热点；按设备 ID 一致性哈希吸附则引入粘性会话与扩缩容漏洞期。为防重放建立全链路粘性，性价比极低。

**结论**：高频上报的重放危害是「数据重复写」，用幂等唯一键在写侧去重，成本只发生在真正的重复请求上；鉴权链路保持纯无状态。

## 目录结构

```
src/main/java/lan/chaos/hmac/
├── HmacAuthDemo.java                    # 入口：串行跑四场景，输出对比表/汇总
├── model/
│   ├── ReportRequest.java               # 上报请求模型(deviceId/path/ts/nonce/batchNo/body/sign)
│   └── VerifyResult.java                # 校验结果(通过/失败原因/耗时)
├── core/
│   ├── HmacSigner.java                  # 签名串规范 + HMAC-SHA256 + 常量时间比较 + nonce 生成
│   └── SecretKeyStore.java              # 密钥双槽(current/previous)，支撑轮换
├── verify/
│   ├── RequestVerifier.java             # 完整校验链：时间窗→签名(current/previous)→防重放
│   └── ReplayGuard.java                 # 滑动窗口频率限制 + 业务幂等去重（0 存储）
├── rotate/
│   └── KeyRotationManager.java          # 双密钥过渡轮换(新签发+旧验签宽限期)
└── bench/
    ├── ThroughputBenchmark.java         # 场景D：本地验签 vs 模拟Redis读 吞吐/p99对比
    └── RedisReadSimulator.java          # 模拟 Redis 网络往返延迟(可配)
src/test/java/lan/chaos/hmac/
├── HmacSignerTest.java                  # 正确签名/篡改/错误密钥
├── RequestVerifierTest.java             # 时间窗/重放/幂等去重
└── KeyRotationManagerTest.java          # 宽限期/切换后拒绝
```

## 运行方式

```bash
# 编译并跑全部单元测试
mvn -q -pl jdk8-tech/jdk8-hmac-auth-demo test

# 跑四场景演示（输出吞吐对比表）
mvn -q -pl jdk8-tech/jdk8-hmac-auth-demo compile
java -cp jdk8-tech/jdk8-hmac-auth-demo/target/classes lan.chaos.hmac.HmacAuthDemo
```

## 技术要点

- **签名串规范化**：按行拼接 `method\npath\ntimestamp\nnonce\nbodyDigest`，防拼接歧义；bodyDigest 用 SHA-256 保证请求体完整性。
- **常量时间比较**：验签用 `MessageDigest.isEqual`，防时序侧信道。
- **时间戳时间窗**：`|now - ts| <= skew`（默认 300s），无状态、天然水平扩展，仅需 NTP 同步。
- **双密钥轮换**：current 签发、previous 宽限期验签，平滑切换不中断在途设备；宽限期结束广播新密钥后丢弃旧钥。
- **吞吐主线**：消除每请求网络往返（1ms Redis 读 vs 0 往返）→ QPS 数量级提升，呼应 Q1 的 1000+→10000+。
