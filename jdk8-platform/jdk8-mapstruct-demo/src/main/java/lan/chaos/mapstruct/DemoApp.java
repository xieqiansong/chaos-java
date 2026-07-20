package lan.chaos.mapstruct;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;
import lan.chaos.mapstruct.basic.AddressDto;
import lan.chaos.mapstruct.basic.BasicMapper;
import lan.chaos.mapstruct.basic.UserDto;
import lan.chaos.mapstruct.collection.CollectionMapper;
import lan.chaos.mapstruct.custom.CustomMapper;
import lan.chaos.mapstruct.custom.UserCardDto;
import lan.chaos.mapstruct.custom.UserSummaryDto;
import lan.chaos.mapstruct.common.model.Address;
import lan.chaos.mapstruct.common.model.Gender;
import lan.chaos.mapstruct.common.model.Role;
import lan.chaos.mapstruct.common.model.User;
import lan.chaos.mapstruct.nested.NestedMapper;
import lan.chaos.mapstruct.nested.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 演示入口：依次展示各「能力分类」包中的映射用法。直接 {@code mvn exec:java} 或运行 main 即可。
 */
public class DemoApp {

    public static User sampleUser() {
        return User.builder()
                .id("1")
                .username("root")
                .password("root")
                .email("root@confusion.lan")
                .phone("12388888888")
                .realName("管理员")
                .gender(Gender.MALE)
                .role(Role.ADMIN)
                .birthday(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2026, 7, 19, 10, 0, 0))
                .enabled(true)
                .level("VIP")
                .address(Address.builder()
                        .province("广东省")
                        .city("深圳市")
                        .detail("南山区科技园 1 号")
                        .build())
                .build();
    }

    public static void main(String[] args) {
        User user = sampleUser();

        Console.log("=== basic: clone（深拷贝） ===");
        Console.log(JSONUtil.toJsonStr(BasicMapper.INSTANCE.clone(user)));

        Console.log("=== basic: toDto（基础+嵌套）/ fromDto（反向） ===");
        UserDto dto = BasicMapper.INSTANCE.toDto(user);
        Console.log(JSONUtil.toJsonStr(dto));
        Console.log(JSONUtil.toJsonStr(BasicMapper.INSTANCE.fromDto(dto)));

        Console.log("=== collection: toDtoList ===");
        List<UserDto> list = CollectionMapper.INSTANCE.toDtoList(Arrays.asList(user, user));
        Console.log(JSONUtil.toJsonStr(list));

        Console.log("=== nested: 自包含嵌套映射 ===");
        Person person = Person.builder()
                .name("张三")
                .address(lan.chaos.mapstruct.nested.Address.builder()
                        .street("科技园路 1 号")
                        .city("深圳")
                        .build())
                .build();
        Console.log(JSONUtil.toJsonStr(NestedMapper.INSTANCE.toDto(person)));

        Console.log("=== custom: toSummary（字段名/忽略/常量/默认值/表达式） ===");
        UserSummaryDto summary = CustomMapper.INSTANCE.toSummary(user);
        Console.log(JSONUtil.toJsonStr(summary));

        Console.log("=== custom: toCard（嵌套/日期格式化/qualifiedByName） ===");
        UserCardDto card = CustomMapper.INSTANCE.toCard(user);
        Console.log(JSONUtil.toJsonStr(card));
    }
}
