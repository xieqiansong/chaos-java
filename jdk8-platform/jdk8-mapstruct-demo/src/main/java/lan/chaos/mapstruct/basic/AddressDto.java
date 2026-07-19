package lan.chaos.mapstruct.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 地址 DTO（字段与 {@link lan.chaos.mapstruct.model.Address} 同名），用于演示嵌套对象自动映射。
 * 同时被 {@code custom} 包的卡片 DTO 复用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto implements Serializable {
    private String province;
    private String city;
    private String detail;
}
