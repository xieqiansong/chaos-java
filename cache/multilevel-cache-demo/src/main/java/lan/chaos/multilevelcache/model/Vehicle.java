package lan.chaos.multilevelcache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 车辆实体（演示用，通用资源/车辆语义，字段面向多级缓存场景）。
 * 作为多级缓存的缓存值类型。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 车辆 ID(业务主键)。 */
    private Long vehicleId;
    /** 车牌号。 */
    private String plateNo;
    /** 车辆状态：1=可用 2=维修 3=报废。 */
    private Integer status;
    /** 所属部门。 */
    private String department;
    /** 当前 GPS 经度(高频变化字段，用于演示「值变化->版本号变化」)。 */
    private String gpsLng;
    /** 当前 GPS 纬度。 */
    private String gpsLat;
}
