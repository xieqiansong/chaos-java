package lan.chaos.mapstruct.custom;

import lan.chaos.mapstruct.basic.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 卡片 DTO，用于演示自定义映射中的：
 * <ul>
 *     <li>嵌套对象映射（address -> AddressDto）</li>
 *     <li>日期格式化（dateFormat）</li>
 *     <li>自定义类型转换（qualifiedByName）</li>
 *     <li>表达式拼接（expression）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCardDto implements Serializable {
    private String username;
    // 由 Gender 枚举转中文（qualifiedByName）
    private String genderDesc;
    // 由 Role 枚举转中文（qualifiedByName）
    private String roleDesc;
    // 由 LocalDate 格式化为字符串（dateFormat）
    private String birthdayStr;
    // 由 LocalDateTime 格式化为字符串（dateFormat）
    private String createdAtStr;
    // 嵌套地址映射
    private AddressDto address;
    // 由嵌套 address 拼接而成的完整地址（expression）
    private String addressDetail;
}
