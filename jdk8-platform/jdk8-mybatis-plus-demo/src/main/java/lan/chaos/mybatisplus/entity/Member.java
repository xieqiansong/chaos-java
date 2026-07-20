package lan.chaos.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lan.chaos.mybatisplus.common.handler.AesTypeHandler;
import lombok.Data;

/**
 * 会员实体，用于字段级 AES 加密演示。
 * phone 标了 AesTypeHandler：落库即密文，读取自动还原明文，业务无感。
 *
 * 关键：autoResultMap = true。MyBatis-Plus 默认的结果映射不会带上字段级 typeHandler，
 * 只在「写入参数」时加密；设了 autoResultMap 后，MP 会为该类单独生成含 typeHandler 的结果映射，
 * 使 selectById 等读取时也能自动解密。切忌用 type-handlers-package 全局扫描注册，
 * 那会把 AesTypeHandler 注册成「全局 String 处理器」，导致所有 VARCHAR 字段（如 name）被误加密。
 */
@Data
@TableName(value = "member", autoResultMap = true)
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;
}
