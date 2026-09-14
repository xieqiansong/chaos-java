package lan.chaos.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Paxos算法模拟
 * 包含三种角色：Proposer、Acceptor、Learner
 */
public class PaxosSimulation {
    
    // 消息类型枚举
    enum MessageType {
        PREPARE, PROMISE, ACCEPT, ACCEPTED, LEARN
    }
    
    // 消息类
    static class Message {
        MessageType type;
        int fromId;           // 发送者ID
        int toId;             // 接收者ID
        int proposalId;       // 提案编号
        int acceptedId;       // 已接受的提案编号
        Object value;         // 提案值
        int promiseCount;     // Promise计数
        
        public Message(MessageType type, int fromId, int toId, 
                      int proposalId, Object value) {
            this.type = type;
            this.fromId = fromId;
            this.toId = toId;
            this.proposalId = proposalId;
            this.value = value;
        }
    }
    
    // 网络层 - 模拟消息传递
    static class Network {
        private Map<Integer, BlockingQueue<Message>> nodeQueues = new ConcurrentHashMap<>();
        private Random random = new Random();
        private final double messageLossRate = 0.1;  // 10%消息丢失率
        
        public void registerNode(int nodeId) {
            nodeQueues.put(nodeId, new LinkedBlockingQueue<>());
        }
        
        public void send(Message msg) throws InterruptedException {
            // 模拟消息丢失
            if (random.nextDouble() < messageLossRate) {
                return;
            }
            
            // 模拟网络延迟
            Thread.sleep(random.nextInt(50));
            
            BlockingQueue<Message> queue = nodeQueues.get(msg.toId);
            if (queue != null) {
                queue.put(msg);
            }
        }
        
        public Message receive(int nodeId) throws InterruptedException {
            BlockingQueue<Message> queue = nodeQueues.get(nodeId);
            if (queue != null) {
                return queue.poll(100, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    }
    
    // Acceptor角色
    static class Acceptor implements Runnable {
        private int id;
        private Network network;
        private volatile boolean running = true;
        private ReentrantLock lock = new ReentrantLock();
        
        // Acceptor状态
        private int promisedId = 0;      // 承诺的提案编号
        private int acceptedId = 0;      // 已接受的提案编号
        private Object acceptedValue = null;  // 已接受的值
        
        public Acceptor(int id, Network network) {
            this.id = id;
            this.network = network;
        }
        
        @Override
        public void run() {
            System.out.println("Acceptor " + id + " 启动");
            
            while (running) {
                try {
                    Message msg = network.receive(id);
                    if (msg != null) {
                        processMessage(msg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        private void processMessage(Message msg) throws InterruptedException {
            lock.lock();
            try {
                switch (msg.type) {
                    case PREPARE:
                        handlePrepare(msg);
                        break;
                    case ACCEPT:
                        handleAccept(msg);
                        break;
                }
            } finally {
                lock.unlock();
            }
        }
        
        private void handlePrepare(Message msg) throws InterruptedException {
            if (msg.proposalId > promisedId) {
                promisedId = msg.proposalId;
                
                // 发送Promise响应
                Message promise = new Message(MessageType.PROMISE, id, msg.fromId, 
                                            msg.proposalId, acceptedValue);
                promise.acceptedId = acceptedId;
                network.send(promise);
                
                System.out.println("Acceptor " + id + " 承诺提案 " + msg.proposalId);
            }
        }
        
        private void handleAccept(Message msg) throws InterruptedException {
            if (msg.proposalId >= promisedId) {
                promisedId = msg.proposalId;
                acceptedId = msg.proposalId;
                acceptedValue = msg.value;
                
                // 发送Accepted响应
                Message accepted = new Message(MessageType.ACCEPTED, id, msg.fromId, 
                                             msg.proposalId, msg.value);
                network.send(accepted);
                
                System.out.println("Acceptor " + id + " 接受提案 " + msg.proposalId + " 值: " + msg.value);
            }
        }
        
        public void stop() {
            running = false;
        }
    }
    
    // Proposer角色
    static class Proposer implements Runnable {
        private int id;
        private Network network;
        private List<Integer> acceptorIds;
        private volatile boolean running = true;
        private AtomicInteger proposalCounter = new AtomicInteger(0);
        private Random random = new Random();
        
        // 提案状态
        private Object proposedValue;
        private int currentProposalId;
        private Map<Integer, Message> promises = new ConcurrentHashMap<>();
        private int acceptCount = 0;
        private boolean valueChosen = false;
        
        public Proposer(int id, Network network, List<Integer> acceptorIds) {
            this.id = id;
            this.network = network;
            this.acceptorIds = acceptorIds;
        }
        
        @Override
        public void run() {
            System.out.println("Proposer " + id + " 启动");
            
            while (running && !valueChosen) {
                try {
                    // 随机休眠，模拟不同Proposer的启动时间
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // 开始一轮Paxos
                    startNewProposal();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        private void startNewProposal() throws InterruptedException {
            // 生成唯一的提案编号
            currentProposalId = proposalCounter.incrementAndGet() * 100 + id;
            proposedValue = "Value from Proposer " + id + "-" + currentProposalId;
            
            System.out.println("\nProposer " + id + " 开始新提案: " + currentProposalId + " 值: " + proposedValue);
            
            // 阶段1: Prepare阶段
            promises.clear();
            
            // 向所有Acceptor发送Prepare请求
            for (int acceptorId : acceptorIds) {
                Message prepare = new Message(MessageType.PREPARE, id, acceptorId, 
                                            currentProposalId, null);
                network.send(prepare);
            }
            
            // 等待Promise响应
            int promisesNeeded = acceptorIds.size() / 2 + 1;  // 多数派
            long timeout = System.currentTimeMillis() + 2000;
            
            while (promises.size() < promisesNeeded && System.currentTimeMillis() < timeout) {
                Message msg = network.receive(id);
                if (msg != null && msg.type == MessageType.PROMISE && 
                    msg.proposalId == currentProposalId) {
                    promises.put(msg.fromId, msg);
                    
                    // 如果有Acceptor已经接受了值，使用那个值
                    if (msg.value != null) {
                        proposedValue = msg.value;
                        System.out.println("Proposer " + id + " 使用已接受的值: " + proposedValue);
                    }
                }
                Thread.sleep(10);
            }
            
            if (promises.size() >= promisesNeeded) {
                // 阶段2: Accept阶段
                acceptCount = 0;
                
                for (int acceptorId : acceptorIds) {
                    Message accept = new Message(MessageType.ACCEPT, id, acceptorId, 
                                               currentProposalId, proposedValue);
                    network.send(accept);
                }
                
                // 等待Accepted响应
                timeout = System.currentTimeMillis() + 2000;
                while (acceptCount < promisesNeeded && System.currentTimeMillis() < timeout) {
                    Message msg = network.receive(id);
                    if (msg != null && msg.type == MessageType.ACCEPTED && 
                        msg.proposalId == currentProposalId) {
                        acceptCount++;
                        
                        if (acceptCount >= promisesNeeded) {
                            // 达到多数派，值被选定
                            valueChosen = true;
                            System.out.println("✅ Proposer " + id + " 提案 " + currentProposalId + 
                                             " 达成共识! 值: " + proposedValue);
                            
                            // 通知Learners
                            notifyLearners();
                            return;
                        }
                    }
                    Thread.sleep(10);
                }
            }
            
            System.out.println("Proposer " + id + " 提案 " + currentProposalId + " 失败，将重试");
        }
        
        private void notifyLearners() throws InterruptedException {
            // 在实际Paxos中，这里会通知Learners
            // 简化为直接发送给所有节点
            for (int i = 0; i < 5; i++) {  // 假设有5个Learner
                Message learn = new Message(MessageType.LEARN, id, i, 
                                          currentProposalId, proposedValue);
                network.send(learn);
            }
        }
        
        public void stop() {
            running = false;
        }
    }
    
    // Learner角色
    static class Learner implements Runnable {
        private int id;
        private Network network;
        private volatile boolean running = true;
        private Map<Integer, Integer> acceptCounts = new ConcurrentHashMap<>();
        private Map<Integer, Object> learnedValues = new ConcurrentHashMap<>();
        
        public Learner(int id, Network network) {
            this.id = id;
            this.network = network;
        }
        
        @Override
        public void run() {
            System.out.println("Learner " + id + " 启动");
            
            while (running) {
                try {
                    Message msg = network.receive(id);
                    if (msg != null) {
                        processMessage(msg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        private void processMessage(Message msg) {
            if (msg.type == MessageType.LEARN) {
                int count = acceptCounts.getOrDefault(msg.proposalId, 0) + 1;
                acceptCounts.put(msg.proposalId, count);
                
                if (count >= 3) {  // 假设需要3个接受才能学习
                    if (!learnedValues.containsKey(msg.proposalId)) {
                        learnedValues.put(msg.proposalId, msg.value);
                        System.out.println("🎓 Learner " + id + " 学习到值: " + msg.value);
                    }
                }
            }
        }
        
        public void stop() {
            running = false;
        }
    }
    
    // 主测试类
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== Paxos分布式共识算法模拟 ==========\n");
        
        // 创建网络
        Network network = new Network();
        
        // 创建并启动Acceptors
        List<Integer> acceptorIds = Arrays.asList(101, 102, 103, 104, 105);
        List<Acceptor> acceptors = new ArrayList<>();
        List<Thread> acceptorThreads = new ArrayList<>();
        
        for (int id : acceptorIds) {
            network.registerNode(id);
            Acceptor acceptor = new Acceptor(id, network);
            acceptors.add(acceptor);
            Thread thread = new Thread(acceptor, "Acceptor-" + id);
            acceptorThreads.add(thread);
            thread.start();
        }
        
        // 创建并启动Proposers
        List<Integer> proposerIds = Arrays.asList(201, 202, 203);
        List<Proposer> proposers = new ArrayList<>();
        List<Thread> proposerThreads = new ArrayList<>();
        
        for (int id : proposerIds) {
            network.registerNode(id);
            Proposer proposer = new Proposer(id, network, acceptorIds);
            proposers.add(proposer);
            Thread thread = new Thread(proposer, "Proposer-" + id);
            proposerThreads.add(thread);
        }
        
        // 创建并启动Learners
        List<Integer> learnerIds = Arrays.asList(301, 302, 303, 304, 305);
        List<Learner> learners = new ArrayList<>();
        List<Thread> learnerThreads = new ArrayList<>();
        
        for (int id : learnerIds) {
            network.registerNode(id);
            Learner learner = new Learner(id, network);
            learners.add(learner);
            Thread thread = new Thread(learner, "Learner-" + id);
            learnerThreads.add(thread);
            thread.start();
        }
        
        // 启动Proposers
        for (Thread thread : proposerThreads) {
            thread.start();
        }
        
        // 运行一段时间
        Thread.sleep(10000);
        
        System.out.println("\n========== 停止模拟 ==========");
        
        // 停止所有线程
        for (Proposer proposer : proposers) {
            proposer.stop();
        }
        for (Acceptor acceptor : acceptors) {
            acceptor.stop();
        }
        for (Learner learner : learners) {
            learner.stop();
        }
        
        // 等待线程结束
        for (Thread thread : proposerThreads) {
            thread.join(1000);
        }
        for (Thread thread : acceptorThreads) {
            thread.join(1000);
        }
        for (Thread thread : learnerThreads) {
            thread.join(1000);
        }
        
        System.out.println("\n模拟结束");
    }
}