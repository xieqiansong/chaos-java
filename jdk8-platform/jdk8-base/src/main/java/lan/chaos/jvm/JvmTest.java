package lan.chaos.jvm;

import cn.hutool.core.thread.ThreadUtil;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JvmTest {
    /**
     * -XX:MaxMetaspaceSize=10M
     */
    public static void testMetaSpaceOOM() {

        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(HeapOOM.class);
            enhancer.setUseCache(false);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                    return methodProxy.invoke(o, objects);
                }
            });
            enhancer.create();

        }
    }

    /**
     * -Xmx256M
     *
     * @param interval
     */
    public static void testOom1(int interval) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            Map<String, Object> map = new HashMap<>();
            for (int j = 0; j < i; j++) {
                map.put(String.valueOf(j), j);
            }
            mapList.add(map);
            ThreadUtil.sleep(interval);
        }
    }


    /*
    java -XX:NativeMemoryTracking=detail -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M -XX:+AlwaysPreTouch -XX:ReservedCodeCacheSize=128m -XX:InitialCodeCacheSize=128m -Xss512k -Xmx4g -Xms4g -XX:+UseG1GC -XX:G1HeapRegionSize=4M -jar ./confusion-example.jar

    scp ./confusion-example.jar root@REDACTED:~/test
     */
    public static void main(String[] args) {
        testOom1(100);
    }
}
