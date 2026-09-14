package lan.chaos.mybatisplus.common.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.mybatisplus.common.model.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 高阶：在自定义 SQL 中拼接 Wrapper 片段（${ew.customSqlSegment}）。
     * 这样既能复用 MP 的条件构造能力，又能在复杂联表 / 特殊语法下手写 SQL，
     * 比纯 Wrapper 灵活、比纯 @Select 好维护。
     */
    @Select("SELECT * FROM t_user ${ew.customSqlSegment}")
    List<User> selectByWrapper(@Param("ew") Wrapper<User> wrapper);
}
