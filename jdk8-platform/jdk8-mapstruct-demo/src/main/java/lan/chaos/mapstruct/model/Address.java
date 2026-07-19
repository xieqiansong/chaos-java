package lan.chaos.mapstruct.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 地址，作为 {@link User} 的嵌套对象（被 basic / custom 复用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address implements Serializable {
    private String province;
    private String city;
    private String detail;
}
