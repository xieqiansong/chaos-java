package lan.chaos.mybatisplus.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lan.chaos.mybatisplus.entity.User;
import lan.chaos.mybatisplus.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 场景一：条件构造器 Wrapper 高阶用法。
 * 覆盖：apply 安全拼接 SQL 片段（占位符 + 参数绑定，避免字符串拼接注入）、
 *      like 模糊查询、排序，以及「在自定义 SQL 中复用 Wrapper 片段」(${ew.customSqlSegment})。
 */
@Service
public class WrapperScenario {

    @Resource
    private UserMapper userMapper;

    /**
     * 组合条件：age between 18 and 40 且 name 含 'a'。
     * 演示 apply 占位符 + 参数绑定（防注入），与 like 模糊查询。
     * 对应单测：断言结果全部 age∈[18,40] 且 name 含 'a'。
     */
    public List<User> complexQuery() {
        QueryWrapper<User> w = new QueryWrapper<>();
        w.apply("age between {0} and {1}", 18, 40)
         .like("name", "a")
         .orderByAsc("id");
        return userMapper.selectList(w);
    }

    /**
     * 自定义 SQL + Wrapper 片段：手写 SELECT，但把条件构造交给 MP。
     * 对应 UserMapper.selectByWrapper 的 ${ew.customSqlSegment}。
     * 这里要求 age > 20，便于单测断言「结果全部 age > 20」。
     */
    public List<User> customSqlWithWrapper() {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.gt(User::getAge, 20).orderByAsc(User::getId);
        return userMapper.selectByWrapper(w);
    }
}
