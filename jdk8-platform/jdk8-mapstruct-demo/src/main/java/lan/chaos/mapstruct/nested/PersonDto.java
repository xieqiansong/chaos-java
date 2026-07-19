package lan.chaos.mapstruct.nested;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 含嵌套 {@link AddressDto} 的目标 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonDto implements Serializable {
    private String name;
    private AddressDto address;
}
