package lan.chaos.starter.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lan.chaos.starter.common.constant.StarterConstant;

/**
 * Starter 可外部化配置（核心机制之一：@ConfigurationProperties）。
 *
 * <p>WHY：starter 的价值在于「约定优于配置」——提供合理默认值让用户开箱即用，
 * 同时允许通过 application.yml 覆盖。这类绑定类就是「用户侧配置 → 容器内 Bean」的桥梁。</p>
 *
 * <p>关键 API：{@link ConfigurationProperties} 绑定前缀 {@code token.starter}。
 * 配合 {@code spring-boot-configuration-processor} 会在编译期为 IDE 生成
 * spring-configuration-metadata.json，实现配置项自动补全。</p>
 *
 * <p>生产坑：绑定类通常用 {@code @ConstructorBinding} + {@code final} 字段做不可变绑定；
 * 本 demo 为降低噪音用 {@code @Data} + setter 绑定（2.7 默认支持）。</p>
 */
@Data
@ConfigurationProperties(prefix = StarterConstant.CONFIG_PREFIX)
public class TokenProperties {

    /** 总开关：false 时不装配 TokenService（演示 @ConditionalOnProperty）。 */
    private boolean enabled = true;

    /** 生成 token 的长度（默认 32）。 */
    private int length = 32;

    /** 生成 token 的前缀（默认空）。 */
    private String prefix = "";

    /** 字符集：simple=仅字母数字，hex=十六进制（默认 simple）。 */
    private Charset charset = Charset.SIMPLE;

    public enum Charset {
        SIMPLE,
        HEX
    }
}
