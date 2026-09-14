package lan.chaos.mybatisplus.page;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lan.chaos.mybatisplus.common.mapper.OrderMapper;
import lan.chaos.mybatisplus.common.mapper.UserMapper;
import lan.chaos.mybatisplus.common.model.OrderUserVO;
import lan.chaos.mybatisplus.common.model.User;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 场景二：分页。
 * 演示 单表分页（selectPage）与 联表分页（自定义 SQL + IPage，MP 自动 count）。
 */
@Service
public class PageScenario {

    @Resource
    private UserMapper userMapper;
    @Resource
    private OrderMapper orderMapper;

    public IPage<User> userPage(long current, long size) {
        return userMapper.selectPage(new Page<>(current, size), null);
    }

    public IPage<OrderUserVO> orderUserPage(long current, long size) {
        // 注意：OrderUserVO 是结果 VO、不是表实体，LambdaQueryWrapper 无法为它建立 lambda 缓存，
        // 因此这里用普通 QueryWrapper + 列名字符串；${ew.customSqlSegment} 同样能拼出 ORDER BY。
        // 排序列用 t_order 的真实列 create_time，并用表别名 o 限定，避免联表时列名歧义
        // （t_order 与 t_user 都有 create_time，不限定会报 Ambiguous column）。
        QueryWrapper<OrderUserVO> w = new QueryWrapper<>();
        w.orderByDesc("o.create_time");
        return orderMapper.selectOrderUserPage(new Page<>(current, size), w);
    }
}
