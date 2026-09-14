package lan.chaos.tools;

public class CPULoadController {

    /**
     * 控制单CPU负载保持在指定比例
     *
     * @param targetLoad 目标负载比例 (0-100)
     */
    public static void controlCPULoad(int targetLoad) {
        if (targetLoad < 0 || targetLoad > 100) {
            throw new IllegalArgumentException("负载比例必须在0-100之间");
        }

        // 将百分比转换为小数
        double load = targetLoad / 100.0;

        // 采样间隔（毫秒）
        long sampleInterval = 1000;

        System.out.println("开始控制CPU负载为: " + targetLoad + "%");

        while (true) {
            long startTime = System.currentTimeMillis();

            // 忙等待阶段 - 消耗CPU
            while ((System.currentTimeMillis() - startTime) < sampleInterval * load) {
                // 执行一些计算密集型操作来占用CPU
                performCalculation();
            }

            // 睡眠阶段 - 释放CPU
            try {
                long sleepTime = (long) (sampleInterval * (1 - load));
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 更精确的负载控制方法（基于时间窗口）
     *
     * @param targetLoad 目标负载比例 (0-100)
     * @param timeWindow 时间窗口大小（毫秒）
     */
    public static void controlCPULoadPrecise(int targetLoad, long timeWindow) {
        if (targetLoad < 0 || targetLoad > 100) {
            throw new IllegalArgumentException("负载比例必须在0-100之间");
        }

        double load = targetLoad / 100.0;
        System.out.println("精确控制CPU负载为: " + targetLoad + "%, 时间窗口: " + timeWindow + "ms");

        long busyTime = (long) (timeWindow * load);
        long idleTime = timeWindow - busyTime;

        while (true) {
            long startTime = System.nanoTime();

            // 忙等待
            while ((System.nanoTime() - startTime) < busyTime * 1_000_000L) {
                performCalculation();
            }

            // 空闲等待
            try {
                if (idleTime > 0) {
                    Thread.sleep(idleTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 计算密集型操作
     */
    private static void performCalculation() {
        // 使用多种数学运算来确保CPU被有效占用
        double result = 0;
        for (int i = 0; i < 1000; i++) {
            result += Math.sin(i) * Math.cos(i) + Math.sqrt(i);
            result -= Math.log(i + 1);
        }
    }

    /**
     * 动态调整的负载控制（适应系统变化）
     *
     * @param targetLoad         目标负载比例
     * @param adjustmentInterval 调整间隔（秒）
     */
    public static void controlCPULoadDynamic(int targetLoad, int adjustmentInterval) {
        if (targetLoad < 0 || targetLoad > 100) {
            throw new IllegalArgumentException("负载比例必须在0-100之间");
        }

        double load = targetLoad / 100.0;
        long adjustmentMillis = adjustmentInterval * 1000L;
        long lastAdjustment = System.currentTimeMillis();

        System.out.println("动态控制CPU负载为: " + targetLoad + "%, 调整间隔: " + adjustmentInterval + "s");

        while (true) {
            long windowStart = System.currentTimeMillis();
            long busyTimeTarget = (long) (adjustmentMillis * load);
            long busyTimeActual = 0;

            while (System.currentTimeMillis() - windowStart < adjustmentMillis) {
                long operationStart = System.nanoTime();
                performCalculation();
                long operationEnd = System.nanoTime();

                busyTimeActual += (operationEnd - operationStart) / 1_000_000L;

                // 如果已经超过目标忙时，提前进入空闲
                if (busyTimeActual >= busyTimeTarget) {
                    long remainingTime = adjustmentMillis - (System.currentTimeMillis() - windowStart);
                    if (remainingTime > 0) {
                        try {
                            Thread.sleep(remainingTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    break;
                }
            }

            // 动态调整计算强度
            adjustCalculationIntensity(busyTimeActual, busyTimeTarget);
        }
    }

    /**
     * 根据实际与目标负载的差异调整计算强度
     */
    private static void adjustCalculationIntensity(long actual, long target) {
        double ratio = (double) actual / target;
        // 可以根据比例调整performCalculation中的循环次数
        // 这里只是示例，实际实现可能需要更复杂的调整逻辑
    }
}