package lan.chaos.mybatisplus.common.handler;

import lan.chaos.mybatisplus.common.util.AesUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 字段级 AES 透明加解密 TypeHandler。
 * 配合实体字段 {@code @TableField(typeHandler = AesTypeHandler.class)} 使用：
 * 写入时加密、读取时解密，业务代码完全无感（呼应「安全」短板，敏感字段如手机号落库即密文）。
 *
 * 注意：不要加 @MappedTypes(String.class)/@MappedJdbcTypes，否则会被注册为「全局 String 处理器」，
 * 导致所有 VARCHAR 字段（如 name）都被加解密。本类只通过 type-handlers-package 注册，
 * 仅在显式标注 @TableField(typeHandler=...) 的字段上生效。
 */
public class AesTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, AesUtil.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return AesUtil.decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return AesUtil.decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return AesUtil.decrypt(cs.getString(columnIndex));
    }
}
