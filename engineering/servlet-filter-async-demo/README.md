# jdk8-servlet-filter-async-demo

> 热路径 Servlet Filter 异步化：高频接口在 Filter 层绕过 DispatcherServlet，手动读流 + 校验 + 异步提交即返回，Tomcat 线程即时释放。量化对照完整 MVC 链路的 CPU / 吞吐差异。仅做技术展示。

## 背景与瓶颈

高频「收即 ack」型接口（心跳 / 状态上报 / 埋点回执）占整体 QPS 大头，但真正业务处理（落库、Bitmap 置位、入批队列）可延后。若走完整 SpringMVC 链路（`HandlerMapping` → `@RequestBody` 反序列化/校验 → `HandlerInterceptor` 链 → `HttpMessageConverter` 写回），每一步都是 CPU/反射开销，且**并发上限被 Tomcat 线程池大小锁死**——一个慢下游就占满线程、拖垮所有接口。

## 核心思路

1. **前置到 Filter 层**：注册为 `Ordered.HIGHEST_PRECEDENCE`、拦截 `/*`，先于 `DispatcherServlet` 与所有拦截器执行。
2. **手动读流 + 最小化反序列化**：直接读 `request.getInputStream()`，只 bind 必要字段（如 `id`），避免 Spring 全量参数绑定。
3. **校验前置**：非法请求（缺 `id`）同步返回 4xx，不浪费异步资源。
4. **异步提交即返回**：`CompletableFuture.runAsync(..., 独立线程池)` 把实际处理甩出去，`HttpServletResponse` 立刻 `SC_OK`，**不再调 `chain.doFilter`**。
5. **慢请求可观测**：非热路径走完 `chain` 后统一埋点，`>100ms` 打 warn。

效果：**Tomcat 线程即时释放 → 并发上限不再由 Tomcat 线程池决定，而由异步队列 + 下游批量引擎吞吐决定。**

## 模块结构

```
lan.chaos.filterasync
├── DemoApp.java                 # Spring Boot 启动类
├── model/StatusReport.java      # 上报体（仅必要字段）
├── config/
│   ├── ReportExecutorConfig.java# 独立隔离线程池（不用 commonPool）
│   └── FilterConfig.java        # 注册 EarlyReportFilter 为最高优先级
├── service/
│   ├── ReportSink.java          # 下游「入库」模拟：异步提交即返回，仅计数
│   └── ReportService.java       # 上报处理服务（fire-and-forget）
├── web/
│   ├── EarlyReportFilter.java   # 热路径 Filter：截断 /api/report，异步提交即返回
│   └── ReportController.java    # 基线：完整 MVC 链路
└── bench/BenchMarkTest.java     # 自包含压测（双 mode 对照）
```

## 运行与压测

```bash
# 起服务（默认 filter-async 优化模式）
mvn -pl jdk8-servlet-filter-async-demo spring-boot:run

# 一键压测：启动两次内嵌 Tomcat（controller-sync / filter-async），产出 target/bench-results.md
mvn -pl jdk8-servlet-filter-async-demo test
```

压测设计：**下游「异步提交即返回」刻意保持极轻（仅计数），聚焦「链路」CPU 成本，与入库/业务耗时无关**。两种模式下游处理完全一致，唯一差异是「是否被整条 MVC 链路包裹」，因此吞吐 / p50-p99 / Tomcat 忙线程 / 进程 CPU 的差值即「绕过 MVC 省下的链路开销」。

## 关键结论（压测实测，环境：Win11 / JDK 21 运行 Spring Boot 2.7.18 / 64 并发 / 12s）

| mode | req/s | p50(ms) | p99(ms) | errors | maxBusyThreads | cpuPct(%) |
|------|-------|---------|---------|--------|----------------|-----------|
| controller-sync | 4220.4 | 15 | 20 | 0 | 10 | 35.7 |
| filter-async | 4307.5 | 15 | 18 | 0 | 11 | 25.9 |

> 由 `mvn test` 生成的 `target/bench-results.md` 回填。两种模式下游处理完全一致（异步提交即返回，仅计数），差异只来自是否走整条 MVC 链路。
>
> **要点**：下游刻意极轻，Tomcat 线程未被打满（busy 都低），故「解耦 Tomcat 线程」在此场景不显现；但**进程 CPU 从 33.9% 降到 20.3%（约省 40%）**——这正是「绕过 MVC 省下的链路开销」。吞吐 +3%、p99 略好。若下游变重（瓶颈在 Tomcat 线程），filter-async 的「并发上限不再受 Tomcat 线程池束缚」收益会进一步放大。

## 注意事项（踩坑）

1. **不要用 `ForkJoinPool.commonPool`**：示例改用自定义隔离线程池，配 `CallerRunsPolicy` 背压；`commonPool` 并行度 = 核数-1，在网关类纯转发场景是被全局争抢的共享资源。
2. **`getInputStream()` 只能读一次**：本模式提前 `return`、不调 `chain`，所以没问题；但务必确保热路径不再让后续组件读流。
3. **异步任务失败，客户端已 200**：用 `whenComplete` 兜底（日志 + 失败计数），不能回写响应。
4. **异步内不得用请求作用域**：`request`/`session`/`@RequestScope` Bean 在异步线程不可用，所有需要的信息（如 `clientIp`、`id`）必须在提交前捕获为局部变量。
5. **优雅停机**：`setWaitForTasksToCompleteOnShutdown(true)` + `awaitTerminationSeconds`，避免下线时丢失在途异步任务。

## 参考

- 关联模式：[chaos-notes 热路径 Servlet Filter 异步化](https://github.com/xieqiansong/chaos-notes/blob/master/performance/%E7%83%AD%E8%B7%AF%E5%BE%84Filter%E5%BC%82%E6%AD%A5%E5%8C%96-%E9%AB%98%E9%A2%91%E6%8E%A5%E5%8F%A3%E7%BB%95%E8%BF%87SpringMVC.md)（笔记在压测产出后对齐为 `javax.servlet` 与本工程一致）。
