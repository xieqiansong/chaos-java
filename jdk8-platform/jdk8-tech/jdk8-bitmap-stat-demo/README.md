# jdk8-bitmap-stat-demo

Bitmap 统计演示模块（纯 JDK8、零运行时依赖）：承接 1200 万设备接入场景，解决「数据落库后的统计」——在线状态 / 日活用位图压缩存储 + O(1) 聚合，完整演示大 key 位运算的瓶颈与拆分优化，并以 HyperLogLog 作 UV 统计的「精确 vs 近似」对比方案。

## 定位演进

```
M1 jdk8-hmac-auth-demo（已完成）：上报链路鉴权——HMAC 无状态签名，干掉每请求 Redis 读
M4 本模块（本次）：              数据统计——Bitmap 每设备 1 bit，省内存 + 快聚合 + 大 key 拆分
                                 + HLL 对比方案：UV 近似统计（恒定 12 KB vs Bitmap 线性增长）
```

痛点：1200 万设备若用 `Set<String>`/Hash 存状态 → 数百 MB~GB；Bitmap → **≈1.43 MB**。统计在线数/日活时 `BITCOUNT` 是 CPU 位运算（`Long.bitCount`），内存连续、cache 友好；UV 这类量级统计再换 HLL，**恒定 ~12 KB**、误差 ~0.81%。

## 六个场景

| 场景 | 内容 | 演示点 |
|------|------|--------|
| A | 在线状态维护 | `SETBIT` 上下线、`GETBIT` 查询、`BITCOUNT` 实时在线数（1200 万设备中 300 万在线） |
| B | 日活统计 | 按天一个位图，`BITOP OR` 得近 N 天活跃、`AND` 得连续活跃（按天分 key = 时间维度分片） |
| C | 内存账对比 | 1200 万设备：Bitmap ≈1.43 MB vs `byte[]` 12 MB vs `Set<String>` ~1.2 GB |
| D | 吞吐对比 | GETBIT vs Set.contains 千万级查询；BITCOUNT vs Set.size（cache 友好性） |
| E | 大 key 拆分优化 | 单线程阻塞复现 / 哈希分片 / 区域分片 / 增量计数 |
| F | UV 统计对比 | Bitmap 精确 UV vs HyperLogLog 近似（600 万访问 → 300 万 UV，误差 ~0.81% vs 恒定 12 KB） |

## 场景 E：大 key 位运算瓶颈与拆分优化

### 为什么是瓶颈（两层）

| 层 | 原因 |
|----|------|
| 算法层 | `BITOP` 是 O(N)（N 取较长操作数），`BITCOUNT` 全量扫描 O(N/64) |
| 执行层 | Redis 命令执行是单线程：一次大 key 位运算占住主循环，**后续所有命令整体排队**——危害不是「这条命令慢」，是「整条链路的尾延迟被拉长」 |

### 四种应对

| 方案 | 机制 | 取舍 |
|------|------|------|
| 哈希分片 | `crc32(deviceId) % N` | 数据均匀无热点；全局统计需跨片聚合 |
| 区域分片 | 设备 ID 前缀（地市码）→ 分片 | 统计即分区，单区域命中单片零聚合；区域间可能不均 |
| 增量计数 | `SETBIT 0→1` 时 count++、`1→0` 时 count-- | 在线数/日活 O(1) 直读，完全绕开 BITCOUNT；代价是变更多一次计数维护 |
| 时间分片 | 按天分 key | 天然滚动统计（场景 B） |

## 容量账（1200 万设备）

| 方案 | 单设备 | 1200 万占用 | 说明 |
|------|--------|-------------|------|
| Bitmap（1 bit/设备） | 1 bit | ≈1.43 MB | 连续内存、cache 友好 |
| byte[]（1 B/设备） | 1 B | ≈11.4 MB | 状态/温度等小整型 |
| Set\<String\>（估算） | ≈104 B | ≈1.19 GB | String(15 字符≈72B) + HashMap.Node(≈32B) |
| HyperLogLog（16384 寄存器） | 恒定 | ≈12 KB | 与基数无关，代价是 ~0.81% 误差（UV 这类量级统计） |

## 场景 F：UV 统计对比（Bitmap 精确 vs HyperLogLog 近似）

### 为什么放进来

Bitmap 的精确是有代价的：**内存随基数上界线性增长**（1200 万设备 = 1.43 MB），且必须先知道设备总量才能分配位图。而日活/UV 这类「只关心量级、不要求绝对精确」的指标，用 HLL 以 ~0.81% 误差换**恒定 ~12 KB** 内存，且不需要预知基数上界——二者互为「精确 vs 近似」的对比方案，故合并在同一模块。

### 实现要点（`probabilistic/HyperLogLog`，纯 JDK8 自实现）

| 点 | 机制 |
|----|------|
| 哈希 | MurmurHash3 x64（取 low 64 位），分布均匀是估算准确的前提 |
| 寄存器定位 | 低 p 位 → 寄存器下标；其余位从最高位起数连续 0 + 1 作为 rank，保留历史最大 |
| 估算 | 调和平均 E = α·m² / Σ 2^(-reg) |
| 小基数修正 | E ≤ 2.5m 且存在空寄存器时用 LinearCounting（m·ln(m/V)），否则系统性低估 |
| 打包 | 本实现 1 B/寄存器（16 KB，便于理解）；Redis 式 6-bit 打包 = 12288 B ≈ 12 KB |

### 与 Redis 语义对照

`PFADD` ≈ `pfadd(element)`，`PFCOUNT` ≈ `pfcount()`。Redis HLL 还支持 `PFMERGE`（按天 key 合并为周/月 UV，无需重放数据），本 Demo 未实现，属于进阶方向。

## 目录结构

```
jdk8-bitmap-stat-demo/
├── pom.xml                                  # JDK8、零运行时依赖、仅 junit(test)
├── README.md                                # 定位演进 + 五场景 + 容量账 + 运行方式
└── src/
    ├── main/java/lan/chaos/bitmap/
    │   ├── BitmapStatDemo.java              # 入口：串行跑五场景，输出对比表/汇总
    │   ├── core/
    │   │   ├── BitMap.java                  # 自实现 long[] 位集：SETBIT/GETBIT/BITCOUNT/BITOP
    │   │   └── BitMapBenchmark.java         # 场景 D：吞吐对比工具
    │   ├── probabilistic/
    │   │   └── HyperLogLog.java             # 场景 F：自实现 HLL（murmur3 + pfadd/pfcount + 修正）
    │   ├── bigkey/
    │   │   ├── BigKeyShardScene.java        # 场景 E：瓶颈复现 + 三种拆分/计数优化
    │   │   ├── HashSharder.java             # 哈希分片（crc32 % N）
    │   │   ├── RegionSharder.java           # 区域分片（前缀映射）
    │   │   ├── BitMapWithCounter.java       # 增量计数位图（SETBIT + count 维护）
    │   │   └── SingleThreadQueueSimulator.java # Redis 单线程队列模拟（尾延迟）
    │   ├── stat/
    │   │   ├── OnlineStatusStat.java        # 场景 A
    │   │   ├── DailyActiveStat.java         # 场景 B
    │   │   ├── MemoryAccountStat.java       # 场景 C
    │   │   └── UvStatCompare.java           # 场景 F：Bitmap 精确 UV vs HLL 近似
    │   └── model/
    │       └── StatResult.java              # 统计结果模型
    └── test/java/lan/chaos/bitmap/
        ├── BitMapTest.java                  # 位操作正确性/边界/扩容/BITOP
        ├── DailyActiveStatTest.java         # OR/AND 语义断言
        ├── BigKeyShardSceneTest.java        # 分片确定性/计数一致性/尾延迟改善
        └── HyperLogLogTest.java             # 基数估算误差带/去重语义/恒定内存
```

## 运行方式

```bash
# 单元测试（验证位操作、BITOP、分片与计数语义）
mvn -pl jdk8-tech/jdk8-bitmap-stat-demo test

# 六场景演示（A 在线状态 / B 日活 / C 内存账 / D 吞吐 / E 大 key 拆分 / F UV 对比）
java -cp jdk8-tech/jdk8-bitmap-stat-demo/target/classes lan.chaos.bitmap.BitmapStatDemo
```

## 技术要点

1. **底层换算**：`words[offset >>> 6]` 定位所在 long、`1L << (offset & 63)` 定位位——与 Redis 的 byte 粒度（`offset >>> 3`）同思路，64 位宽位运算吞吐更高
2. **BITCOUNT**：逐 long `Long.bitCount` 累加，O(N/64) 且无分支，硬件 POPCNT 加速
3. **BITOP**：OR/AND 逐 word 位运算，结果为新位图（原对象不变，可安全复用）
4. **大 key 拆分**：哈希分片（均匀）/区域分片（统计即分区）的取舍；增量计数把在线数查询从 O(N) 降到 O(1)
5. **单线程模型**：`SingleThreadQueueSimulator` 用「命令完成时间 = 前方累计」模拟 Redis 主循环，量化大命令对尾延迟的拖累
6. **HLL vs Bitmap**：精确 vs 近似的取舍——Bitmap 精确但内存随基数上界线性增长、需预知设备总量；HLL 恒定 ~12 KB、误差 ~0.81%，只依赖寄存器数。面试要点：误差与数据规模无关、hash 质量决定上限、小基数需 LinearCounting 修正、6-bit 打包
