package lan.chaos.distributed.system.paxos.demo;
// ==================== 示例用法 ====================

import lan.chaos.distributed.system.paxos.node.PaxosNode;

public class BasicPaxosDemo {
    public static void main(String[] args) throws InterruptedException {
        int clusterSize = 3;
        PaxosNode node0 = new PaxosNode(0, clusterSize, 2000);
        PaxosNode node1 = new PaxosNode(1, clusterSize, 2000);
        PaxosNode node2 = new PaxosNode(2, clusterSize, 2000);

        // 节点 0 发起提议
        byte[] result = node0.propose("Hello, Paxos".getBytes());
        System.out.println("Proposed value: " + new String(result));

        // 等待 Learner 学习
        Thread.sleep(1000);
        System.out.println("Node1 learned: " + new String(node1.getLearnedValue()));
        System.out.println("Node2 learned: " + new String(node2.getLearnedValue()));
    }
}
