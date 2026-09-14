# Nacos Server（启动与配置学习）

本目录不是业务代码模块，而是 **Nacos 服务端本身的启动与配置样例**，方便你在本机一键拉起一个 Nacos，理解它的启动参数、端口与持久化配置。

> 其它 demo（`nacos-provider` / `nacos-consumer` / `nacos-config`）默认连 `REDACTED:8848`，所以跑这些 demo 前，先在本目录把 Nacos 起起来。

## 需要什么？

| 项 | 说明 |
|----|------|
| JDK | **8 或 11**（Nacos 2.2.x 推荐，不要用 JDK 17+ 以免踩坑） |
| 内存 | 默认 JVM 堆 2g；学习机可在 `startup` 或 `docker-compose` 里调到 256m |
| 下载 | 见下方「方式一」需要下载 `nacos-server` 包；「方式二」Docker 不用下载 |
| 数据库 | **不需要**：学习用内置 Derby，零外部依赖；生产才切 MySQL（见 `conf/application.properties.example`） |

## 方式一：本机二进制（需要下载 tar.gz / zip）

1. 下载（选一个，版本对齐 2.x，推荐 `2.2.3`）：
   - Windows：`nacos-server-2.2.3.zip`
   - Linux / macOS：`nacos-server-2.2.3.tar.gz`
   - 地址：<https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.zip>
2. 解压到本目录下的 `nacos/`（即 `nacos-server/nacos/bin/startup.cmd`）。
3. 启动（**单机模式**必须加 `-m standalone`）：
   ```bash
   # Windows
   .\scripts\start.cmd
   # Linux / macOS
   bash ./scripts/start.sh
   ```
4. 控制台：<http://REDACTED:8848/nacos> 账号/密码 `nacos` / `nacos`。

> 直接下载解压后也能用 Nacos 自带的 `bin/startup.cmd -m standalone`，`scripts/start.*` 只是把它包装了一下并约定目录。

## 方式二：Docker（无需下载，推荐）

```bash
# 在本目录执行
docker compose up -d
```

`docker-compose.yml` 已映射 `8848 / 9848 / 9849` 三个端口，并以 `standalone` 模式运行，JVM 内存调到 256m 适合笔记本。

```bash
# 看日志
docker compose logs -f nacos
# 停止
docker compose down
```

## 关键启动配置（学习重点）

| 配置 | 作用 | 在哪改 |
|------|------|--------|
| `MODE=standalone` | 单机模式（学习用）；集群模式才用 `cluster` | `startup -m standalone` / compose `environment` |
| `server.port=8848` | HTTP 控制台与配置/注册 API 端口 | `conf/application.properties` |
| `8848 / 9848 / 9849` | 8848=HTTP；9848=gRPC 客户端→服务端；9849=服务端→服务端（**2.x 新增，容器必须映射**） | 端口 / compose `ports` |
| `cluster.conf` | 集群节点列表（单机用不到，集群才配） | `conf/cluster.conf` |
| 数据源 | 默认 Derby（内置，零依赖）；生产改 MySQL | `conf/application.properties`（见 `application.properties.example`） |
| JVM 内存 | `JVM_XMS` / `JVM_XMX` / `JVM_XMN` | `startup` 脚本 / compose `environment` |

示例配置文件放在 `conf/`：`application.properties.example`（端口、数据源切换）、`cluster.conf.example`（集群格式）。
要自定义时，把 `.example` 复制为 `application.properties` / `cluster.conf`，并在 `docker-compose.yml` 里加一行挂载即可（见文件内注释）。

## 与 demo 的配合

1. 先启动本 Nacos（方式一或二）。
2. 再启动 `nacos-provider` + `nacos-consumer` 看服务注册发现；启动 `nacos-config` 看配置动态刷新。
3. `sentinel-demo` 的场景 6（Nacos 动态规则源）也连同一个 `8848`，在 `application.yml` 设 `sentinel.dynamic.nacos.enabled=true`。
