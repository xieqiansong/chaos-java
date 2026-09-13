package lan.chaos.common.core.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrganizationEntity implements Serializable {
    private String id;
    private String parentId;
    private String orgName;
    private String orgCode;
    private Integer sortCode = 0;
    private Byte deleteFlag;
}