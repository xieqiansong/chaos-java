# java-basic-demo — Java 基础内功

一句话定位：Java 语言与运行时内功：反射 / 异常 / 泛型 / 注解 / 集合源码 / SPI / JDK+CGLib 动态代理 / 手写迷你 Servlet 容器 / JMH 字符串拼接基准。

## 场景一览

| 主题 | 代表类 | 测试 |
|---|---|---|
| 反射 | `java.base.HelloReflect` | — |
| 异常 | `java.base.ExceptionDemo` | — |
| 泛型擦除 | `java.base.generic.GenericTypeErasureDemo` | `GenericTypeErasureTest` |
| 注解 | `java.base.annotation.CustomAnnotationDemo` | `CustomAnnotationTest` |
| 集合源码 | `java.base.collection.HashMapSourceAnalysis` / `ArrayListSourceAnalysis` / `ConcurrentHashMapSourceAnalysis` | `CollectionSourceTest` |
| SPI | `java.spi.SPIDemo`（`ServiceLoader` + `META-INF/services`） | — |
| 动态代理 | `proxy.ProxyDemo`（JDK 接口代理 + CGLib 继承代理 + CallbackFilter/LazyLoader） | — |
| 迷你 Servlet 容器 | `simple.web.HttpServer` / `MiniHttpServer` | — |
| JMH 基准 | `SimpleBenchmark` / `RealisticConcatBenchmark` | — |

## 快速开始

IDE 运行对应 `main`。

```bash
mvn -pl java-core/java-basic-demo test
```

> 注：`CollectionSourceTest` 用反射访问 `java.util` 内部字段（ArrayList/HashMap），JDK9+ 运行时需加 `--add-opens java.base/java.util=ALL-UNNAMED`；JDK8 下可直接跑。

## 设计要点

依赖 Hutool / CGLib / JMH。SPI 与 agent 的 `META-INF` 资源随本模块保留。本模块自原 `jdk8-base` 拆分而来，包名保持不变。
