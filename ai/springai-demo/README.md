# jdk17-springai-demo

Spring AI 学习模块。按 **「能力组 / 场景 / 单元测试」** 组织：每个场景一个 Service（能力实现）+ 一个测试（验证与演示），
让"学到什么"和"怎么验证"一一对应。

- JDK 17 + Spring Boot 3.5.14 + Spring AI 1.1.0
- 父 POM：`jdk17-platform`，模块名 `jdk17-springai-demo`

## 目录结构

```
src/main/java/lan/chaos/springai/
├── SpringAiApplication.java             # 启动类
├── common/config/ChatClientConfig.java  # 公共 ChatClient（统一默认系统提示词）
├── common/config/VectorStoreConfig.java # 向量库选型：默认内存版 / pgvector profile 切 PgVector
├── chat/                                # chat 能力组
│   ├── basic/BasicChatService.java      # 同步对话
│   ├── stream/StreamChatService.java    # 流式输出
│   ├── memory/MemoryChatService.java    # 多轮记忆（窗口式）
│   └── prompt/PromptTemplateService.java# 提示词模板
├── output/                              # output 能力组
│   ├── ActorFilms.java                  # 结构化输出目标类型（record）
│   └── StructuredOutputService.java     # Bean/List 结构化输出
├── tools/                               # tools 能力组
│   ├── DateTimeTools.java               # 工具定义（@Tool）
│   └── ToolCallingService.java          # 工具调用编排
├── rag/                                 # rag 能力组
│   └── RagService.java                  # 检索增强生成（读取/切分/入库/检索/生成）
└── mcp/                                 # mcp 能力组（客户端侧）
    └── McpChatService.java              # 调用远端 MCP 服务端的工具
```

> 测试语料：`src/test/resources/rag/chaos-knowledge.txt`（一份模型训练数据里没有的私有文档，
> 专门用来验证 RAG「让模型回答它本来不知道的内容」的价值）。
>
> **配套 MCP 服务端**：同级独立模块 `jdk17-mcp-server-demo`（端口 30052，SSE 传输）。
> MCP 是跨进程协议，服务端与客户端必须分开部署——这正是它能被多个 AI 应用共享的前提。

## 快速开始

模型端点支持**两种模式**，业务代码与测试完全不用改（DeepSeek 兼容 OpenAI 协议，只换连接信息）。

### 模式一：本地 llama.cpp（默认，零成本、离线）

```bash
llama-server.exe -m <model.gguf> --port 30040 -c 8192 -n 2048 -ngl 99 -t 8 -np 1 --reasoning off
mvn -pl jdk17-springai-demo test
```

> `--reasoning off` 很关键：本地推理模型的思考过程会挤占输出预算（见踩坑记录）。

RAG 场景还需要一个 **embedding 服务**（把文本转成向量；DeepSeek 不提供 embedding API，因此无论对话走云端还是本地，embedding 都用本地模型）：

```bash
llama-server.exe -m <bge-m3.gguf> --port 30041 --embeddings --ubatch-size 2048
```

未启动 embedding 服务时，RAG 测试自动跳过，其余测试不受影响。

MCP 场景还需要启动 **MCP 服务端**（工具跑在独立进程里）：

```bash
cd jdk17-mcp-server-demo
mvn -q package -DskipTests
java -jar target/jdk17-mcp-server-demo-1.0-SNAPSHOT.jar    # 端口 30052

# 客户端需要叠加 mcp profile（默认关闭，原因见踩坑记录）
mvn -pl jdk17-springai-demo test -Dspring.profiles.active=deepseek,mcp -Dtest=McpTest
```

MCP 服务端未启动、或未启用 `mcp` profile 时，MCP 测试自动跳过，其余测试不受影响。

### 向量库切换：内存版 ↔ PgVector

RAG 的存储层统一走 `VectorStore` 接口，换存储**不改一行业务代码**。默认用内存版 `SimpleVectorStore`（零依赖、测试友好、重启即丢）；
启用 `pgvector` profile 后切换为 `PgVectorStore` 持久化到 Postgres：

```bash
# 密码只走环境变量，禁止写入 yml（避免随仓库泄露）
$env:PG_PASSWORD="<你的密码>"
mvn -pl jdk17-springai-demo test -Dspring.profiles.active=deepseek,pgvector -Dtest=RagTest
```

未启用 `pgvector` profile 时，RAG 测试照常走内存版，不受影响（见踩坑记录）。

### 模式二：云端 DeepSeek（稳定、高速）

```powershell
$env:DEEPSEEK_API_KEY="sk-xxxx"   # 只走环境变量，禁止写入 yml
mvn -pl jdk17-springai-demo test -Dspring.profiles.active=deepseek
```

两种模式的实测对比：

| 测试 | 本地 Qwen3.5-9B | DeepSeek v4-flash |
|---|---|---|
| 多轮记忆 | ❌ 277s 后失败 | ✅ 1.9s |
| 流式输出 | ❌ 35.9s（空流） | ✅ 1.7s |
| 冒烟对话 | 17.9s（不稳定） | ✅ 2.3s |

## 测试约定

- **真实模型测试**：统一调用 `ModelEndpoint.assumeUp()` 探活，环境不具备时 assumption 失败 → 测试**跳过而非失败**，保证 CI 无外部依赖也能通过
- **纯单元测试**（模板渲染、记忆窗口截断）：不依赖任何外部服务，永远执行

## 踩坑记录（Spring AI 1.1.0）

| 问题 | 现象 | 结论 / 解法 |
|---|---|---|
| 推理模型长思考 | 本地模型思考占满 token，`content` 为空、流式返回空块 | llama.cpp 的 `-n` 是**总输出**（含思考）；服务端加 `--reasoning off`。DeepSeek 的 `max_tokens` 只限最终输出，思考单独计量，故不受影响 |
| `extraBody` 不生效 | 想用 `chat_template_kwargs` 关思考，带与不带耗时一致 | 1.1.0 中 `OpenAiChatModel` **未消费** `OpenAiChatOptions.extraBody`（字节码中无 `getExtraBody` 调用），无法下发自定义请求字段 |
| 模板语法报错 | `{#if}` / `{var:默认值}` 解析失败 | 模板引擎是 ST4：变量 `{name}`、条件 `{if(cond)}...{endif}`，**无**默认值语法 |
| `AssistantMessage#getContent()` 不存在 | 编译失败 | 1.1.0 统一为 `getText()` |
| 记忆 Advisor 构造失败 | `new MessageChatMemoryAdvisor(chatMemory)` 报参数不匹配 | 改用 `MessageChatMemoryAdvisor.builder(chatMemory).build()` |
| 请求 404 | `/v1/v1/chat/completions` | `base-url` 只填根地址，Spring AI 的 `completions-path` 自带 `/v1` |
| 工具调用"没生效" | 最终响应的 `getToolCalls()` 是空的 | 工具调用发生在**中间轮次**：框架拿结果回填后，最终 `AssistantMessage` 只剩文本。应改用"回复里出现真实数据"（如当前年份）来验证，见 `ToolCallingTest` |
| 工具参数变成 `arg0` | Function Calling 参数绑定失败 | `@Tool` 依赖编译期参数名，需 `-parameters`（本模块 pom 已配置 `<parameters>true</parameters>`） |
| 检索结果全是重复片段 | RAG 检索返回重复内容，命中不了目标片段 | `SimpleVectorStore` 在同一测试上下文内是共享的，`@BeforeEach` 反复 `ingest` 会累积重复文档。改用 `@TestInstance(PER_CLASS)` + `@BeforeAll` 只入库一次 |
| MCP 客户端拖垮整个应用 | 未启动服务端时**所有**测试全红 | 客户端启动时会主动连服务端，连不上会让 Spring 上下文启动失败。默认 `spring.ai.mcp.client.enabled=false`，用 `mcp` profile 启用；测试探活需同时校验 profile 与端口 |
| MCP 服务端探活总失败 | 用 HTTP 请求 `/sse` 判断存活必然超时 | `/sse` 是长连接，HTTP 请求会一直挂起。改用 TCP 端口探测，见 `ModelEndpoint#isMcpServerUp` |
| PgVector 默认就激活 | 加依赖后所有测试全红（强依赖数据源） | 开关是 `spring.ai.vectorstore.type=pgvector` 且 `matchIfMissing=true`；不设反而默认开。默认设 `type: simple` 关闭 |
| PgVector 拖出 Hikari 报错 | 无数据源配置时上下文启动失败 | pgvector starter 引入 spring-jdbc，触发 `DataSourceAutoConfiguration`。主类排除它，pgvector profile 下由 `VectorStoreConfig` 显式提供 `DataSource` |
| jdbcUrl 缺失 | PgVector 报 "jdbcUrl is required" | 裸 `DataSourceBuilder` 不会把 `spring.datasource.url` 翻译成 Hikari 的 `jdbcUrl`；改用 `DataSourceProperties#initializeDataSourceBuilder()` |
| pgvector 检索命中不到 | 持久化表跨运行累积重复文档，目标片段被挤出 topK | 测试 `@BeforeAll` 里 `TRUNCATE TABLE vector_store` 再入库（仅 pgvector 模式；内存版为无操作） |

## 学习进度

| 能力组 | 场景 | Service | 测试 | 状态 |
|---|---|---|---|---|
| chat | 同步对话 | `BasicChatService` | 真实调用 | ✅ |
| chat | 流式输出 | `StreamChatService` | 真实流式 | ✅ |
| chat | 多轮记忆 | `MemoryChatService` | 真实两轮 + 窗口截断单测 | ✅ |
| chat | 提示词模板 | `PromptTemplateService` | 真实对话 + 渲染单测 | ✅ |
| output | 结构化输出 | `StructuredOutputService` | 真实模型绑定 + Converter 解析单测 | ✅ |
| tools | 工具调用 | `ToolCallingService` | 真实模型调用 + 工具方法单测 | ✅ |
| rag | 检索增强生成 | `RagService` | 真实检索问答 + 切分单测 | ✅ |
| rag | 生产向量库（PgVector） | `RagService` + `VectorStoreConfig` | 真实持久化检索（需 pgvector profile） | ✅ |
| mcp | MCP 协议 | `McpChatService` + 独立服务端模块 | 真实远程工具调用（需 mcp profile） | ✅ |
