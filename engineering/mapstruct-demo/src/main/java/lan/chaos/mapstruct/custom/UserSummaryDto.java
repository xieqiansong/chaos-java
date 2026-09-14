package lan.chaos.mapstruct.custom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 摘要 DTO，用于演示自定义映射中的多种配置：
 * <ul>
 *     <li>字段名不一致映射（source / target）</li>
 *     <li>字段忽略（ignore）</li>
 *     <li>常量填充（constant）</li>
 *     <li>默认值填充（defaultValue）</li>
 *     <li>表达式填充（expression）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto implements Serializable {
    // 来自 User.id（字段名不一致）
    private String userId;
    // 来自 User.username（字段名不一致）
    private String account;
    // 同名映射
    private String email;
    // 被忽略，始终为 null
    private String phone;
    // 常量
    private String type;
    // 默认值：当源 level 为 null 时填充 "NORMAL"
    private String grade;
    // 表达式：拼接真实姓名与用户名
    private String displayName;
}
