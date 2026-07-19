package lan.chaos.mapstruct.nested;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 嵌套目标 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto implements Serializable {
    private String street;
    private String city;
}
