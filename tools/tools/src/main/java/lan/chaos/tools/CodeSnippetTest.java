package lan.chaos.tools;

import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.db.DbUtil;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CodeSnippetTest {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void dataSourceTest() throws SQLException {
        // 连接信息不硬编码：统一从 application.yml 的多数据源配置读取（密码可用环境变量 / application-local.yml 覆盖）
        Console.log("MySQL tables: {}", DbUtil.use(dataSource("mysql")).query("show tables"));
        Console.log("PG tables   : {}", DbUtil.use(dataSource("pg")).query(
                "select tablename from pg_tables where schemaname = 'public'"));
    }

    /** 按名称取数据源：读取 spring.datasource.dynamic.datasource.&lt;name&gt;.* */
    private static DataSource dataSource(String name) {
        String prefix = "spring.datasource.dynamic.datasource." + name + ".";
        return new SimpleDataSource(
                YamlConfig.getRequired(prefix + "url"),
                YamlConfig.getRequired(prefix + "username"),
                YamlConfig.get(prefix + "password", ""));
    }


//    /**
//     * 响应式编程
//     */
//    @SneakyThrows
//    public static void reactorTest() {
//        // 1. 创建简单的序列
//        Flux<String> fruitFlux = Flux.just("Apple", "Banana", "Orange");
//        Mono<String> helloMono = Mono.just("Hello, Reactor!");
//
//        // 2. 进行流式处理：过滤、转换
//        // 过滤出以"A"开头的水果
//        fruitFlux.filter(fruit -> fruit.startsWith("A"))
//                // 将每个元素转换为大写
//                .map(String::toUpperCase)
//                // 订阅并消费数据，输出: APPLE
//                .subscribe(System.out::println);
//
//        // 3. 组合多个序列
//        Flux<String> flux1 = Flux.just("A", "B", "C");
//        Flux<String> flux2 = Flux.just("X", "Y", "Z");
//
//        // 按顺序连接
//        // 结果: A, B, C, X, Y, Z
//        Flux<String> concatenated = Flux.concat(flux1, flux2);
//
//        // 按元素位置合并
//        // 结果: AX, BY, CZ
//        Flux<String> zipped = Flux.zip(flux1, flux2, (e1, e2) -> e1 + e2);
//    }

//    /**
//     * UUID7
//     */
//    public static void uuid7Test() {
//        /*
//
//        <dependency>
//            <groupId>com.github.f4b6a3</groupId>
//            <artifactId>uuid-creator</artifactId>
//            <version>6.1.1</version>
//        </dependency>
//         */
//        UUID timeOrderedEpoch = UuidCreator.getTimeOrderedEpoch();
//        Console.log(timeOrderedEpoch);
//    }

    /**
     * 多个线程循环交替打印1-200
     */
    public void test_20250606() {
        int max = 200;
        AtomicInteger atomicInteger = new AtomicInteger(0);
        List<String> threadList = Arrays.asList("线程1", "线程2", "线程3");
        Map<String, Function<Integer, Boolean>> currentStateMap = new HashMap<>();
        currentStateMap.put("线程1", (i) -> i % threadList.size() == 0);
        currentStateMap.put("线程2", (i) -> i % threadList.size() == 1);
        currentStateMap.put("线程3", (i) -> i % threadList.size() == 2);
        Lock lock = new ReentrantLock();
        for (String threadName : threadList) {
            new Thread(new ThreadGroup(""), () -> {
                lock.lock();
                try {
                    while (atomicInteger.get() < max) {
                        if (currentStateMap.get(threadName).apply(atomicInteger.get())) {
                            Console.log("{} {}", Thread.currentThread().getName(), atomicInteger.incrementAndGet());
                            lock.notifyAll();
                        } else {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }, threadName).start();
        }
        ThreadUtil.sleep(5000);
    }

    /**
     * 100万数据对比
     */
    public void test_20250624() throws InterruptedException {
        HashSet<String> set1 = IntStream.range(0, 10000000).boxed().map(i -> RandomUtil.randomString(5)).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> set2 = IntStream.range(0, 10000000).boxed().map(i -> RandomUtil.randomString(5)).collect(Collectors.toCollection(HashSet::new));
//        Set<String> set2 = Collections.synchronizedSet(new HashSet<>(IntStream.range(0, 10000000).boxed().map(i -> RandomUtil.randomString(5)).collect(Collectors.toSet())));


        int set1_size = set1.size();
        Console.log("{} {}", set1_size, set2.size());

        String[] set1_list = set1.toArray(new String[]{});

        int threads = 16;
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        long startTime = System.currentTimeMillis();


        for (int i = 0; i < set1_size; i += (set1_size / threads)) {
            final int start = i;
            threadPool.submit(() -> {
                for (int j = start; j < start + (set1_size / threads); j++) {
                    if (!set2.contains(set1_list[j])) {
                    } else {
                    }
                }
            });
        }

        threadPool.shutdown();
        while (!threadPool.isTerminated()) {
            // 非阻塞检查，可添加短暂休眠避免CPU空转
            try {
                Thread.sleep(100); // 降低轮询频率
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("所有任务执行完毕");
        Console.log("{}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 创建延时触发器
     */
    public CompletableFuture<Void> schedule(Runnable task, long delay, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.schedule(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, delay, unit);
        return future;
    }

    public void delayTest() {
        AtomicBoolean running = new AtomicBoolean(true);
        schedule(() -> {
            Console.log("_____________________________");
            running.set(false);
        }, 3, TimeUnit.SECONDS);
    }


//    /**
//     * 匿名内部类
//     */
//    public void anonymousInnerClassTest() throws IllegalAccessException {
//        var obj = new Object() {
//            public String name;
//            public String age;
//        };
//        Map<String, String> name = Map.of("name", "ABC", "age", "18");
//        BeanUtil.copyProperties(name, obj);
//
//        Field[] fields = ReflectUtil.getFields(obj.getClass());
//        for (Field field : fields) {
//            Console.log("{}: {}", field.getName(), field.get(obj));
//        }
//    }

//    /**
//     * 学姐吧签到
//     */
//    public static void checkInTest() {
//        // 登录
//        HttpWrapper httpWrapper = HttpWrapper.builder()
//                .baseURL("https://xuejieba2026.com")
//                .build();
//        String username = Config.get("xuejieba.username");
//        String password = Config.get("xuejieba.password");
//        String body = StrUtil.format("code={}&username={}&password={}", username, username, password);
//        String response = httpWrapper.form("/wp-json/jwt-auth/v1/token", "POST", body);
//        String token = JSONUtil.getByPath(JSONUtil.parseObj(response), "token", "");
//
//        // 签到
//        httpWrapper = HttpWrapper.builder()
//                .baseURL("https://xuejieba2026.com")
//                .headers(Collections.singletonMap("Authorization", "Bearer " + token))
//                .build();
//        String missionResponse = httpWrapper.call("/wp-json/b2/v1/userMission", "POST", "");
//        Console.log("签到结果: " + missionResponse);
//    }

    private static void watchMonitor(String dir) {
        WatchMonitor watchMonitor = null;
        try {
            watchMonitor = WatchMonitor.createAll(dir, new SimpleWatcher() {
                @Override
                public void onModify(WatchEvent<?> event, Path currentPath) {
                    Path changedFileName = (Path) event.context();
                    Console.log("EVENT modify: {}", currentPath.resolve(changedFileName).toAbsolutePath());
                }
            });
            watchMonitor.setMaxDepth(16);
            watchMonitor.start();
            Console.log("WatchMonitor started. dir: {}", dir);
        } catch (Exception e) {
            Console.error(e);
        } finally {
            if (Objects.nonNull(watchMonitor)) {
                watchMonitor.close();
            }
        }
    }

    private static void sendUdpMessage() throws IOException {
        String message = "hello udp";
        String host = "127.0.0.1";
        int port = 1234;

        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = message.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(host),
                    port
            );

            socket.send(packet);
            System.out.println("UDP sent");
        }
    }

    public static void httpFollowRedirects() {
        // 使用http协议，抓取百度首页www.baidu.com的html内容（Hutool实现）
        // http 会被 302 重定向到 https，需要开启跟随重定向
        String url = "http://www.baidu.com";
        try (HttpResponse httpResponse = HttpRequest.get(url).setFollowRedirects(true).execute()) {
            String html = httpResponse.body();
            System.out.println(html);
        }
    }

}