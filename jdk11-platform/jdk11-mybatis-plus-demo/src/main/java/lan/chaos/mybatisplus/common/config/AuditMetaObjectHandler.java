package lan.chaos.mybatisplus.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充审计字段：对应 {@code User} 上 @TableField(fill=...) 标注的
 * create_time / update_time / operator。每次 insert / update 由 MP 自动写入，
 * 业务代码不必手动 set，既省心又避免漏填导致审计缺失。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final String OPERATOR = "system-demo";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "operator", String.class, OPERATOR);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "operator", String.class, OPERATOR);
    }
}
