package lan.chaos.tools;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 非 SpringBoot 环境下读取 classpath 下 YAML 配置的工具类（不启动容器）。
 *
 * <p>WHY：全部复用 Spring 原生组件，不自己解析 YAML、不自己拼占位符逻辑：
 * <ul>
 *   <li>{@link YamlPropertySourceLoader}（Spring Boot）：把 application.yml 读成 PropertySource；</li>
 *   <li>{@link StandardEnvironment}（Spring Core）：自带 systemProperties / systemEnvironment，
 *       因此 {@code ${PG_PASSWORD:postgres123}} 占位符、类型转换、必填校验全部白送，
 *       与 SpringBoot 运行时的取值行为一致。</li>
 * </ul>
 *
 * <p>加载优先级（从高到低）：系统属性 &gt; 环境变量 &gt; {@code application-{profile}.yml}
 * &gt; {@code application.yml}。多个 profile 时后者优先。</p>
 *
 * <p>profile 来源：显式 {@code load("local")} &gt; 系统属性/环境变量 {@code spring.profiles.active}
 * &gt; {@code application.yml} 里写的 {@code spring.profiles.active}。</p>
 *
 * <pre>{@code
 * YamlConfig.load("local");                                        // 不传则自动探测
 * String url  = YamlConfig.get("spring.datasource.dynamic.datasource.pg.url");
 * int    port = YamlConfig.getInt("server.port", 8080);
 * String pwd  = YamlConfig.getRequired("spring.datasource.dynamic.datasource.pg.password");
 * }</pre>
 *
 * <p>本类只读、不打印任何配置值。注意：非 SpringBoot 环境下日志默认级别常为 DEBUG，
 * 而 {@link YamlPropertySourceLoader} 在 DEBUG 下会把 YAML 原文（含密码）写进日志，
 * 使用前请把 root 日志级别调到 INFO 及以上。</p>
 */
public final class YamlConfig {

    private YamlConfig() {
    }

    private static final String BASE_NAME = "application";

    private static final String PROFILE_ACTIVE_KEY = "spring.profiles.active";

    private static final YamlPropertySourceLoader YAML_LOADER = new YamlPropertySourceLoader();

    /**
     * 惰性构建，load / refresh 时整体替换。
     */
    private static volatile ConfigurableEnvironment environment;

    private static volatile List<String> activeProfiles = Collections.emptyList();

    // ------------------------------------------------------------------ 加载

    /**
     * 加载配置。不传 profile 时自动探测；重复调用即刷新（整体重建，无残留）。
     *
     * @param profiles 需要激活的 profile，可传多个（后者优先）
     */
    public static synchronized void load(String... profiles) {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();

        // 基础文件先读出来但不加入：它优先级最低，需等 profile 文件先入列
        List<PropertySource<?>> base = loadYaml(BASE_NAME);

        List<String> target = (profiles == null || profiles.length == 0)
                ? detectProfiles(base)
                : normalize(Arrays.asList(profiles));

        // 倒序添加：addLast 越靠后优先级越低，故最后一个 profile 先加、优先级最高
        for (int i = target.size() - 1; i >= 0; i--) {
            for (PropertySource<?> source : loadYaml(BASE_NAME + "-" + target.get(i))) {
                sources.addLast(source);
            }
        }
        for (PropertySource<?> source : base) {
            sources.addLast(source);
        }

        environment = env;
        activeProfiles = Collections.unmodifiableList(target);
    }

    /**
     * 按当前 profile 重新加载。
     */
    public static synchronized void refresh() {
        load(activeProfiles.toArray(new String[0]));
    }

    private static ConfigurableEnvironment env() {
        if (environment == null) {
            synchronized (YamlConfig.class) {
                if (environment == null) {
                    load();
                }
            }
        }
        return environment;
    }

    /**
     * 读取 classpath 下的 {@code name.yml} / {@code name.yaml}，不存在返回空列表。
     */
    private static List<PropertySource<?>> loadYaml(String name) {
        Resource resource = new ClassPathResource(name + ".yml");
        if (!resource.exists()) {
            resource = new ClassPathResource(name + ".yaml");
        }
        if (!resource.exists()) {
            return Collections.emptyList();
        }
        try {
            return YAML_LOADER.load(resource.getFilename(), resource);
        } catch (IOException e) {
            throw new IllegalStateException("加载 YAML 配置失败: " + resource.getFilename(), e);
        }
    }

    /**
     * 用「系统属性 / 环境变量 + application.yml」探测 spring.profiles.active，
     * 因此配置文件里直接写 active 也能识别，与 SpringBoot 行为一致。
     */
    private static List<String> detectProfiles(List<PropertySource<?>> base) {
        StandardEnvironment probe = new StandardEnvironment();
        for (PropertySource<?> source : base) {
            probe.getPropertySources().addLast(source);
        }
        String value = probe.getProperty(PROFILE_ACTIVE_KEY);
        return normalize(StrUtil.isBlank(value)
                ? Collections.<String>emptyList()
                : Arrays.asList(value.split(",")));
    }

    private static List<String> normalize(List<String> profiles) {
        List<String> result = new ArrayList<String>(profiles.size());
        for (String profile : profiles) {
            for (String item : profile.split(",")) {
                if (StrUtil.isNotBlank(item)) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ 读取

    /**
     * 读取字符串配置，未配置返回 null。
     */
    public static String get(String key) {
        return env().getProperty(key);
    }

    /**
     * 读取字符串配置，未配置返回默认值。
     */
    public static String get(String key, String defaultValue) {
        return env().getProperty(key, defaultValue);
    }

    /**
     * 读取必填配置，缺失时抛 IllegalStateException。
     */
    public static String getRequired(String key) {
        return env().getRequiredProperty(key);
    }

    public static int getInt(String key, int defaultValue) {
        return env().getProperty(key, Integer.class, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return env().getProperty(key, Long.class, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return env().getProperty(key, Boolean.class, defaultValue);
    }

    /**
     * 当前生效的 profile。
     */
    public static List<String> profiles() {
        env();
        return activeProfiles;
    }
}
