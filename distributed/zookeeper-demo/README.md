# jdk8-zookeeper-demo  ★ A 类 Demo

ZooKeeper 协调原语演示模块，覆盖**分布式锁（InterProcessMutex）、Leader 选举（LeaderSelector）、配置中心 + Watcher（NodeCache）** 三个高频能力。基于 Curator 客户端，真实连接 ZooKeeper，把每个场景的「输入 → 输出」打印到控制台，可观察、可断言。

- 基础包：`lan.chaos.zookeeper`
- 技术栈：Spring Boot 2.7.18 + Apache Curator 5.5.0（底层 ZooKeeper 3.8）
- 外部依赖：需先 `docker compose up -d` 起一个单机 ZooKeeper；**无 ZK 时测试用 JUnit `Assumptions` 优雅跳过**，保证默认 `mvn test` 全绿
- 形态说明：ZK 本身是分布式协调基础设施，真实形态是多进程。本 demo 用「单客户端 + 控制台 Runner」把三个原语讲清；要观察跨进程争抢/选举，只需在多个终端各跑一次 `ZookeeperDemoApplication.main` 即可

> 使用频率标注：`★★★ 高频`／`◆ 基础`（客户端/常量等支撑模块）。

## 目录结构

```
jdk8-zookeeper-demo/
├── pom.xml                              # 继承 jdk8-platform，curator-recipes + starter-test
├── docker-compose.yml                   # 单机 ZooKeeper 3.8，端口 2181 ◆
├── src/main/resources/application.yml   # 仅应用名与日志级别
└── src/main/java/lan/chaos/zookeeper
    ├── ZookeeperDemoApplication.java    # 启动类：依次演示 锁 / 选主 / 配置
    ├── common/                          # 支撑（常量/工具，非独立业务场景）◆
    │   ├── constant/ZkConstant.java      # 连接串与 znode 路径常量，杜绝魔法值
    │   └── util/ZkClientUtil.java        # Curator 客户端创建 / 连通性探测
    ├── lock/                            # 分布式锁 ★★★
    │   └── LockDemo.java
    ├── leader/                          # Leader 选举 ★★★
    │   └── LeaderDemo.java
    └── config/                          # 配置中心 + Watcher ★★★
        └── ConfigDemo.java
```

> 设计要点：**能力场景是顶层包**（`lock/leader/config`），`constant/util` 这类「配置与支撑」统一收进 `common/`。这正是对后续 A 类 demo 的强制要求。

## 场景一览（按使用频率排序）

`★★★ 高频`
- [分布式锁 lock](#1-分布式锁-lock) → InterProcessMutex 临时顺序节点互斥
- [Leader 选举 leader](#2-leader-选举-leader) → LeaderSelector 选主 + 自动重选
- [配置中心 config](#3-配置中心--watcher-config) → NodeCache 监听配置变更实时推送

`◆ 基础模块`
- [ZkClientUtil 客户端工具](#zkclientutil-客户端工具)
- [ZkConstant 常量](#zkconstant-常量)

---

### ZkClientUtil 客户端工具 `◆`

封装 Curator 客户端的创建（`newClient`）与连通性探测（`isAvailable`，供测试 `Assumptions` 用）。

> 为什么用 Curator 而非原生 ZK 客户端：原生 API 偏底层（Watcher 一次性、无重连），Curator 封装了重连、重试、分布式原语（锁/选主/缓存），生产首选。

---

### ZkConstant 常量 `◆`

集中存放默认连接串 `localhost:2181` 与三个能力的 znode 路径（锁 `/chaos/locks/order`、选主 `/chaos/leader`、配置 `/chaos/config/feature-flag`），避免魔法值散落。

---

### 1. 分布式锁 lock `★★★`

多进程/多实例争抢同一把锁，保证临界区互斥。

- 痛点：单机锁（synchronized/ReentrantLock）只管一个 JVM；分布式部署要外部协调者。
- 机制：ZK 用「临时顺序节点」实现公平互斥锁，抢不到就监听前驱节点，前驱释放再抢（无羊群效应）。
- 关键 API：`InterProcessMutex#acquire` / `#release`。
- 生产坑：必须 `release`（否则靠临时节点在 session 断开时自动释放）；锁粒度要小；高并发短临界区更推荐 Redis 锁（性能更好），ZK 锁胜在强一致与自动释放。

验证：见 `LockDemoTest.withLock_executesCriticalSectionAndReturnsResult`（持锁内工作结果正确返回）。

---

### 2. Leader 选举 leader `★★★`

多实例选一个 Leader 承担「只能一个干」的主任务（全局定时调度、主数据同步、主备切换），其余 standby，失败自动转移。

- 机制：ZK 的顺序临时节点公平裁决谁当选；`autoRequeue()` 让失去领导权后自动重新排队。
- 关键 API：`LeaderSelector` + `LeaderSelectorListenerAdapter#takeLeadership`。
- 生产坑：`takeLeadership` 不返回就一直持有领导权，释放靠 `close()` 中断；领导权转移有「脑裂窗口」，业务要幂等或加租约。

验证：见 `LeaderDemoTest.electAndHold_singleInstance_becomesLeader`（单机环境下本实例当选）。

---

### 3. 配置中心 + Watcher config `★★★`

把配置放 ZK 节点，变更时「推」给所有监听方，动态生效，无需重启。

- 痛点：改配置要重启应用。ZK 节点 + Watcher 让配置集中存储、变更即通知。
- 机制：原生 Watcher 一次性（触发后需重注册），Curator 的 `NodeCache` 帮我们持续监听。
- 关键 API：`client.setData()/getData()` + `NodeCache`。
- 生产坑：Watcher 一次性，漏注册会丢事件——用 NodeCache 省心；要有默认值与本地兜底；单节点有 1MB 上限，超大配置放对象存储 + ZK 存指针。

验证：见 `ConfigDemoTest.readConfig_roundTrip`（读写一致）与 `ConfigDemoTest.watchConfig_receivesUpdatePush`（改值后 Watcher 推送 v2）。

---

## 如何运行

```bash
# 0) 先起 ZooKeeper（单机）
cd jdk8-zookeeper-demo
docker compose up -d

# 1) 跑测试（ZK 就绪后三个场景均会执行并断言；无 ZK 时自动跳过）
mvn -pl jdk8-zookeeper-demo -U test

# 2) 或看控制台「输入→输出」：运行 ZookeeperDemoApplication.main
#    多个终端各跑一次，可观察锁争抢与 Leader 选举的跨进程效果
```

预期（控制台节选，ZK 就绪时）：

```
[zk-lock] 获得锁 /chaos/locks/order，执行临界区
[zk-lock] 释放锁 /chaos/locks/order
[main] 锁内结果=critical-work-done
[zk-leader] 本实例当选为 Leader（path=/chaos/leader），开始执行业务
[main] 是否曾当选 Leader=true
[main] 当前配置=v1
[zk-config] 配置变更为 v2（累计收到 2 次）
```

## 进阶方向（生产考量，未写成独立 Demo）

- `◆` **多进程观察**：开多个终端跑 `ZookeeperDemoApplication.main`，真实看到锁等待、Leader 切换、配置广播。
- `◆` **ZK vs Redis 锁**：ZK 强一致/自动释放（胜在可靠），Redis 高性能（胜在吞吐），按一致性与性能要求取舍。
- `◆` **Curator Cache 进阶**：`PathChildrenCache` / `TreeCache` 监听子节点与整棵子树变更。
- `◆` **生产部署**：ZK 应用 ZooKeeper 集群（奇数节点）而非单机；客户端配 `connectionTimeout` / `sessionTimeout` 与合适重试策略。
- `◆` **权限与安全**：ZK 的 ACL / SASL 鉴权，避免任意客户端改协调数据。

## 设计要点

- **真实连接而非内存模拟**：ZK 的协调语义（顺序节点、Watcher、session）必须连真实节点才能体现，故附 `docker-compose.yml` 并提供「无 ZK 跳过」的测试策略，兼顾可观察与 CI 友好。
- **能力即顶层包**：`lock/leader/config` 各自聚焦一个原语，一个类讲清一个知识点，含 WHY 注释（痛点/关键 API/生产坑）。
- **频率结论**：分布式锁、Leader 选举、配置中心是 ZK 的「三件套」，覆盖绝大多数协调场景；其中锁与配置中心最高频，选主次之（多见于调度/主备）。
