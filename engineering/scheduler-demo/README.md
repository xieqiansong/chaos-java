# jdk8-scheduler-demo  ★ A 类（定时任务）

一句话定位：用一个「样例 Job」横向对比三种 Java 定时任务方案——**Spring `@Scheduled`（单进程轻量）、Quartz（功能完备可持久化）、XXL-JOB（分布式调度中心）**，覆盖触发模型、集群去重、分片、生产坑。除 XXL-JOB 需外部 admin（测试跳过）外，其余零外部依赖。

- 基础包：`lan.chaos.scheduler`
- 技术栈：Spring Boot 2.7.18 + spring-boot-starter-quartz + xxl-job-core 2.4.0（JDK8 用 2.x）
- 验证入口：`src/test/.../*Test`（@Scheduled / Quartz 可断言；XXL-JOB 用反射断言处理器注册，执行端无需 admin）
- 启动类：`SchedulerApplication`（控制台后台每 ~1s 打印 @Scheduled 三类触发）

> 使用频率：`★★★ 高频`。

## 目录结构

```
jdk8-scheduler-demo/
├── pom.xml
└── src/main/java/lan/chaos/scheduler
    ├── SchedulerApplication.java          # 启动类（@EnableScheduling）
    ├── common/model/JobSample.java         # 样例业务数据（sampleJob 工厂）
    ├── schedule/ScheduledDemo.java         # @Scheduled：fixedRate / fixedDelay / cron ★★★
    ├── quartz/QuartzDemo.java              # Quartz 内存调度器（RAMJobStore）★★★
    ├── quartz/SampleQuartzJob.java         # Quartz Job 实现（计数）
    ├── xxljob/XxlJobConfig.java            # XXL-JOB 执行端（配了 admin 才启用）★★★
    └── xxljob/SampleXxlJobHandler.java     # XXL-JOB 任务处理器（@XxlJob 分片/简单）
```

## 场景一览

`★★★ 高频`
- [@Scheduled 单进程轻量](#1-scheduled-单进程轻量) → `fixedRate` / `fixedDelay` / `cron` 三种触发模型
- [Quartz 功能完备](#2-quartz-功能完备可持久化) → Job + Trigger 解耦、内存演示、可持久化/集群
- [XXL-JOB 分布式调度](#3-xxl-job-分布式调度中心) → 调度与执行分离、集群去重、分片广播

---

### 1. @Scheduled 单进程轻量 `★★★`

痛点：周期性小任务（缓存预热、心跳、临时清理）不值得上分布式调度，一个注解搞定。
关键 API：`@EnableScheduling` + `@Scheduled(...)`。

三种触发模型（高频考点）：
- `fixedRate`：以上一次**开始**为基准，固定频率（任务耗时 > 间隔会重叠）。
- `fixedDelay`：以上一次**结束**为基准，结束后等固定间隔（绝不重叠）。
- `cron`：cron 表达式精确控制（如每天 0 点）。

生产坑：默认单线程，长任务阻塞后续；多实例部署会**重复执行**（需集群去重 → 见 XXL-JOB）；`fixedRate` 超时后会「追赶式」连续触发。

验证：`ScheduledDemoTest.manualTick_producesInputOutputAndCounts`（输入→输出 + 计数）；`schedulerActuallyFiresInBackground`（后台 ~2.3s 触发 >= 2 次）。

---

### 2. Quartz 功能完备（可持久化） `★★★`

痛点：`@Scheduled` 不能动态增删任务、不能持久化、错过触发无策略。Quartz 用「Job（做什么）+ Trigger（何时做）」解耦。
关键 API：`SchedulerFactory` → `scheduleJob(JobDetail, Trigger)` → `start()`。

本 demo 用**内存 RAMJobStore**（零外部依赖）演示核心；生产要持久化/集群时换成 `JobStoreTX` + 数据库。
生产坑：必须配 misfire 策略；线程池默认 10，长任务会饿死其他任务；集群需共用数据库 + 同 `instanceName`。

验证：`QuartzDemoTest.inMemorySchedulerFiresRepeatedly`（间隔 1s，2.3s 窗口触发 >= 2 次）。

---

### 3. XXL-JOB 分布式调度中心 `★★★`

痛点：`@Scheduled`/Quartz 在多实例下会重复执行、无法统一管控（启停/重试/分片/告警）。XXL-JOB 把「调度」与「执行」分离：admin 统一分发，天然支持集群去重、分片广播、动态启停。
关键组件：`XxlJobSpringExecutor`（执行端，向 admin 注册）+ `@XxlJob("任务名")` 处理器。

本 demo 仅含**执行端**；admin 是独立 Web 服务（需数据库）。执行端 Bean 用 `@ConditionalOnProperty(xxl.job.admin.addresses)` 包裹——没配 admin 时应用照常启动，测试用反射断言处理器已注册（无需外部依赖）。
生产坑：executor 必须能连 admin；分片任务需自己按 `shardIndex/total` 切数据；任务需幂等。

验证：`XxlJobHandlerTest.handlerExposesExpectedXxlJobMethods`（反射确认 `schedulerDemoShardJob` / `schedulerDemoSimpleJob` 两个处理器）。

---

## 如何运行

```bash
# 1) 跑测试（@Scheduled / Quartz 纯内存；XXL-JOB 仅反射断言，零外部依赖）
mvn -pl jdk8-scheduler-demo test

# 2) 控制台观察 @Scheduled 后台触发：运行 SchedulerApplication.main
#    （或 mvn -pl jdk8-scheduler-demo spring-boot:run）

# 3) 接 XXL-JOB admin（可选）：在 application.yml 配置后，执行端会向 admin 注册
# xxl:
#   job:
#     admin.addresses: http://127.0.0.1:8080/xxl-job-admin
#     executor:
#       appname: xxl-job-executor-scheduler-demo
#       port: 9999
# 然后用 QuartzDemo.main / SampleXxlJobHandler 单独验证逻辑
```

预期（@Scheduled 控制台节选）：
```
[scheduled/fixedRate] 计数=1 | [job=heartbeat] triggeredAt=... -> 已执行
[scheduled/fixedDelay] 计数=1 | [job=cache-warmup] triggeredAt=... -> 已执行
[scheduled/cron] 计数=1 | [job=metrics-report] triggeredAt=... -> 已执行
```

## 进阶方向（生产考量）

- `◆` **动态调度**：`@Scheduled` 不支持运行时改周期；Quartz 可 `scheduler.scheduleJob` 动态增删；XXL-JOB 在 admin 界面/API 动态管理。
- `◆` **持久化与集群**：Quartz `JDBC JobStore`；XXL-JOB admin 自带数据库，executor 水平扩展。
- `◆` **高可用**：admin 本身需集群/DB 共享；executor 多实例时 XXL-JOB 自动去重，Quartz 集群靠 DB 行锁。
- `◆` **可观测**：任务执行结果、耗时、失败告警接入监控（见 `jdk8-monitor-demo`）。

## 设计要点

- **同一样例 Job**：三种方案处理同一个 `JobSample`，对比公平、可断言「输入→输出」。
- **能力即顶层包**：`schedule/quartz/xxljob` 各自聚焦一种方案，一个类讲清一个知识点 + WHY 注释。
- **外部依赖最小化**：Quartz 用内存；XXL-JOB 执行端用条件化 Bean，无 admin 也不阻塞 demo 与测试。
- **频率结论**：单进程轻量用 `@Scheduled`；需动态/持久化用 Quartz；多实例统一管控用 XXL-JOB。
