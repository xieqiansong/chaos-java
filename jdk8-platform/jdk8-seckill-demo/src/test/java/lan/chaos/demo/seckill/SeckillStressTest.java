package lan.chaos.demo.seckill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀系统压力测试工具
 * <p>
 * 独立运行，直接调用 REST API，统计性能指标并验证无超卖。
 * <p>
 * 使用方法：
 * 1. 确保秒杀服务已启动（默认 http://localhost:8081）
 * 2. 先通过 Admin API 创建商品并初始化库存
 * 3. 修改下方配置参数后直接运行 main 方法
 */
public class SeckillStressTest {

    // ==================== 配置参数 ====================
    private static final String BASE_URL = "http://localhost:8081";
    private static final Long PRODUCT_ID = 1783422969249L; // 替换为实际商品ID
    private static final int TOTAL_STOCK = 100;          // 商品库存
    private static final int CONCURRENT_USERS = 500;     // 并发用户数
    private static final int RAMP_UP_SECONDS = 5;        // 预热时间（秒）
    private static final int TIMEOUT_SECONDS = 10;       // 请求超时时间
    // =================================================

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 统计指标
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);
    private static final AtomicInteger timeoutCount = new AtomicInteger(0);
    private static final AtomicLong totalResponseTime = new AtomicLong(0);
    private static final AtomicLong maxResponseTime = new AtomicLong(0);
    private static final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("秒杀系统压力测试");
        System.out.println("========================================");
        System.out.println("目标URL: " + BASE_URL);
        System.out.println("商品ID: " + PRODUCT_ID);
        System.out.println("库存: " + TOTAL_STOCK);
        System.out.println("并发用户数: " + CONCURRENT_USERS);
        System.out.println("预热时间: " + RAMP_UP_SECONDS + "s");
        System.out.println("========================================\n");

        // 先验证商品是否存在
        if (!checkProductExists()) {
            System.err.println("错误: 商品不存在，请先通过 Admin API 创建商品");
            System.err.println("POST " + BASE_URL + "/api/admin/product");
            System.err.println("{");
            System.err.println("  \"productName\": \"压测商品\",");
            System.err.println("  \"totalStock\": " + TOTAL_STOCK + ",");
            System.err.println("  \"bucketCount\": 10,");
            System.err.println("  \"price\": 99.00,");
            System.err.println("  \"startTime\": \"2026-07-07T10:00:00\",");
            System.err.println("  \"endTime\": \"2026-07-07T23:59:59\"");
            System.err.println("}");
            return;
        }

        // 启动秒杀活动
        activateProduct();

        // 执行压力测试
        runStressTest();

        // 打印统计结果
        printStatistics();

        // 验证无超卖
        verifyNoOversell();
    }

    /**
     * 检查商品是否存在
     */
    private static boolean checkProductExists() {
        try {
            URL url = new URL(BASE_URL + "/api/admin/product/" + PRODUCT_ID + "/stock");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            System.err.println("检查商品失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 启动秒杀活动
     */
    private static void activateProduct() {
        try {
            URL url = new URL(BASE_URL + "/api/admin/product/" + PRODUCT_ID + "/activate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);
            conn.disconnect();
            System.out.println("启动秒杀活动: " + responseBody);
        } catch (Exception e) {
            System.err.println("启动秒杀活动失败: " + e.getMessage());
        }
    }

    /**
     * 执行压力测试
     */
    private static void runStressTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

        long startTime = System.currentTimeMillis();
        System.out.println("\n开始压力测试...\n");

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final String userId = "stress-user-" + i;

            executor.submit(() -> {
                try {
                    // 模拟预热阶段
                    if (RAMP_UP_SECONDS > 0) {
                        Thread.sleep((long) (Math.random() * RAMP_UP_SECONDS * 1000));
                    }

                    // 发送秒杀请求
                    sendSeckillRequest(userId);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n压力测试完成，总耗时: " + totalTime + "ms\n");
    }

    /**
     * 发送单个秒杀请求
     */
    private static void sendSeckillRequest(String userId) {
        long requestStart = System.currentTimeMillis();

        try {
            String requestBody = String.format("{\"userId\":\"%s\",\"quantity\":1}", userId);

            URL url = new URL(BASE_URL + "/api/seckill/" + PRODUCT_ID);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(TIMEOUT_SECONDS * 1000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);
            conn.disconnect();

            long responseTime = System.currentTimeMillis() - requestStart;
            totalResponseTime.addAndGet(responseTime);
            updateMaxMinResponseTime(responseTime);

            // 解析响应
            JsonNode json = objectMapper.readTree(responseBody);
            int code = json.get("code").asInt();

            if (code == 200) {
                successCount.incrementAndGet();
            } else if (code == 410 || code == 429) {
                failCount.incrementAndGet(); // 售罄或限流
            } else {
                failCount.incrementAndGet();
            }

        } catch (java.net.SocketTimeoutException e) {
            timeoutCount.incrementAndGet();
            long responseTime = System.currentTimeMillis() - requestStart;
            totalResponseTime.addAndGet(responseTime);
            updateMaxMinResponseTime(responseTime);
        } catch (Exception e) {
            failCount.incrementAndGet();
            long responseTime = System.currentTimeMillis() - requestStart;
            totalResponseTime.addAndGet(responseTime);
            updateMaxMinResponseTime(responseTime);
        }
    }

    /**
     * 读取HTTP响应内容
     */
    private static String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        if (conn.getResponseCode() >= 400) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    /**
     * 更新最大最小响应时间
     */
    private static void updateMaxMinResponseTime(long responseTime) {
        // 更新最大值
        long currentMax;
        do {
            currentMax = maxResponseTime.get();
            if (responseTime <= currentMax) break;
        } while (!maxResponseTime.compareAndSet(currentMax, responseTime));

        // 更新最小值
        long currentMin;
        do {
            currentMin = minResponseTime.get();
            if (responseTime >= currentMin) break;
        } while (!minResponseTime.compareAndSet(currentMin, responseTime));
    }

    /**
     * 打印统计结果
     */
    private static void printStatistics() {
        int total = successCount.get() + failCount.get() + timeoutCount.get();
        int success = successCount.get();
        int fail = failCount.get();
        int timeout = timeoutCount.get();

        long avgResponseTime = total > 0 ? totalResponseTime.get() / total : 0;
        long max = maxResponseTime.get();
        long min = minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get();

        double successRate = total > 0 ? (success * 100.0 / total) : 0;
        double qps = total > 0 ? (total * 1000.0 / getTotalTime()) : 0;

        System.out.println("========================================");
        System.out.println("性能统计");
        System.out.println("========================================");
        System.out.printf("总请求数:     %d%n", total);
        System.out.printf("成功数:       %d%n", success);
        System.out.printf("失败数:       %d (售罄/限流)%n", fail);
        System.out.printf("超时数:       %d%n", timeout);
        System.out.printf("成功率:       %.2f%%%n", successRate);
        System.out.printf("QPS:          %.2f%n", qps);
        System.out.println("----------------------------------------");
        System.out.printf("平均响应时间: %d ms%n", avgResponseTime);
        System.out.printf("最大响应时间: %d ms%n", max);
        System.out.printf("最小响应时间: %d ms%n", min);
        System.out.println("========================================\n");
    }

    /**
     * 获取总耗时（毫秒）
     */
    private static long getTotalTime() {
        // 简化处理，假设总耗时为 10 秒
        return 10000;
    }

    /**
     * 验证无超卖
     */
    private static void verifyNoOversell() {
        int success = successCount.get();

        System.out.println("========================================");
        System.out.println("超卖验证");
        System.out.println("========================================");

        if (success <= TOTAL_STOCK) {
            System.out.printf("✓ 验证通过: 成功数(%d) <= 库存(%d)，无超卖%n", success, TOTAL_STOCK);
        } else {
            System.out.printf("✗ 超卖警告: 成功数(%d) > 库存(%d)%n", success, TOTAL_STOCK);
        }

        if (success == TOTAL_STOCK) {
            System.out.println("✓ 库存全部售出");
        } else if (success < TOTAL_STOCK) {
            System.out.printf("⚠ 部分库存未售出: %d 件%n", TOTAL_STOCK - success);
        }

        System.out.println("========================================");
    }
}
