package lan.chaos.mapstruct.nested;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 嵌套源对象（自包含示例，独立于此 demo 其它包）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address implements Serializable {
    private String street;
    private String city;
}
