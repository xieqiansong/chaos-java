package lan.chaos.mapstruct.nested;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 含嵌套 {@link Address} 的实体（自包含示例）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person implements Serializable {
    private String name;
    private Address address;
}
