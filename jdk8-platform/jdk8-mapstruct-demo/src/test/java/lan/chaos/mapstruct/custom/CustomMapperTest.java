package lan.chaos.mapstruct.custom;

import lan.chaos.mapstruct.DemoApp;
import lan.chaos.mapstruct.common.model.Address;
import lan.chaos.mapstruct.common.model.Gender;
import lan.chaos.mapstruct.common.model.Role;
import lan.chaos.mapstruct.common.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 自定义映射测试：验证 {@link CustomMapper} 中「非同名 / 忽略 / 常量 / 默认值 / 表达式 / 枚举转义 / 日期格式化 / 嵌套」等核心机制。
 * 直接复用 {@link DemoApp#sampleUser()} 的样例数据，无需自行准备输入。
 */
class CustomMapperTest {

    private final User user = DemoApp.sampleUser();

    @Test
    void toSummary_mapsRenameIgnoreConstantExpression() {
        UserSummaryDto dto = CustomMapper.INSTANCE.toSummary(user);

        // 字段名不一致：source -> target 重命名映射
        assertEquals(user.getId(), dto.getUserId());
        assertEquals(user.getUsername(), dto.getAccount());
        // 同名映射仍生效
        assertEquals(user.getEmail(), dto.getEmail());
        // @Mapping(target = "phone", ignore = true) → 始终为 null
        assertNull(dto.getPhone());
        // @Mapping(target = "type", constant = "SUMMARY") → 常量填充
        assertEquals("SUMMARY", dto.getType());
        // 源 level 非空（"VIP"），取源值而非 defaultValue
        assertEquals("VIP", dto.getGrade());
        // @Mapping(target = "displayName", expression = ...) → 表达式拼接
        assertEquals(user.getRealName() + "(" + user.getUsername() + ")", dto.getDisplayName());
    }

    @Test
    void toSummary_defaultValueAppliesWhenSourceNull() {
        // 关键坑点：defaultValue 仅在「源字段为 null」时生效，空串/0 不算
        User noLevel = User.builder()
                .id("1").username("root").realName("管理员").email("root@example.com")
                .gender(Gender.MALE).role(Role.ADMIN)
                .birthday(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2026, 7, 19, 10, 0, 0))
                .address(Address.builder().province("广东省").city("深圳市").detail("南山区科技园 1 号").build())
                .level(null)
                .build();

        assertEquals("NORMAL", CustomMapper.INSTANCE.toSummary(noLevel).getGrade());
    }

    @Test
    void toCard_mapsQualifiedDateFormatAndNested() {
        UserCardDto dto = CustomMapper.INSTANCE.toCard(user);

        // @Named("genderToDesc") / @Named("roleToDesc") 经 qualifiedByName 引用 → 枚举转中文
        assertEquals("男", dto.getGenderDesc());
        assertEquals("管理员", dto.getRoleDesc());
        // @Mapping(dateFormat = "yyyy-MM-dd") → LocalDate 格式化
        assertEquals("1990-01-01", dto.getBirthdayStr());
        // LocalDateTime 格式化（含时分秒）
        assertEquals("2026-07-19 10:00:00", dto.getCreatedAtStr());
        // 嵌套对象自动递归映射
        assertEquals(user.getAddress().getCity(), dto.getAddress().getCity());
        // 表达式拼接完整地址
        assertEquals(user.getAddress().getProvince() + user.getAddress().getCity() + user.getAddress().getDetail(),
                dto.getAddressDetail());
    }
}
