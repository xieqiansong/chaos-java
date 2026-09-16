# consensus-demo — 一致性算法（Paxos + Raft）

一句话定位：最小可跑 Basic Paxos（Proposer / Acceptor / Learner + 本地传输）与 Raft 选举 / 日志复制确定性模拟。

## 场景一览

| 算法 | 入口 | 测试 |
|---|---|---|
| Basic Paxos | `distributed.system.paxos.demo.BasicPaxosDemo`（main） | — |
| Raft 选举 / 日志复制 | `distributed.system.raft.RaftElectionDemo` / `RaftReplicationDemo`（main） | `RaftConsensusTest`（6 条断言，内存状态机） |

## 快速开始

```bash
mvn -pl distributed/consensus-demo test
```

IDE 运行 `BasicPaxosDemo` 观察一轮 Prepare/Accept/Chosen 共识过程。

## 设计要点

纯 JDK 实现、零外部依赖。Paxos 走完整角色划分（`message` / `node` / `role` / `state` / `transport`），Raft 用内存状态机做确定性模拟以便断言。本模块自原 `jdk8-base` 拆分而来，包名保持不变。
