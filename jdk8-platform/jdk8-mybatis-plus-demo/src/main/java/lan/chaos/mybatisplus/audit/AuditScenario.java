package lan.chaos.mybatisplus.audit;

import lan.chaos.mybatisplus.common.enums.UserStatusEnum;
import lan.chaos.mybatisplus.common.model.User;
import lan.chaos.mybatisplus.common.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 场景三：逻辑删除 + 乐观锁 + 自动填充（审计字段）。
 * 演示一次「写入→更新(乐观锁)→逻辑删除」全链路，并验证各特性生效。
 */
@Service
public class AuditScenario {

    @Resource
    private UserMapper userMapper;

    public Map<String, Object> logicDeleteAndVersion() {
        // 1) 插入：createTime/updateTime/operator 由 MetaObjectHandler 自动填充
        User u = new User();
        u.setName("Frank");
        u.setAge(25);
        u.setStatus(UserStatusEnum.NORMAL);
        userMapper.insert(u);
        Long id = u.getId();

        // 2) 更新（乐观锁：带 version 条件并自增）
        User before = userMapper.selectById(id);
        before.setName("Frank2");
        int updatedRows = userMapper.updateById(before);
        User after = userMapper.selectById(id);

        // 3) 逻辑删除（生成 UPDATE ... SET deleted=1，而非物理删除）
        int deletedRows = userMapper.deleteById(id);
        User afterDelete = userMapper.selectById(id); // 逻辑删除后不可见

        Map<String, Object> result = new HashMap<>();
        result.put("insertedId", id);
        result.put("updatedRows", updatedRows);
        result.put("versionAfterUpdate", after.getVersion());
        result.put("deletedRows", deletedRows);
        result.put("afterDeleteVisible", afterDelete != null);
        result.put("operatorFilled", after.getOperator());
        return result;
    }
}
