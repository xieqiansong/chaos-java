package lan.chaos.mybatisplus.common.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import lan.chaos.mybatisplus.common.model.Order;
import lan.chaos.mybatisplus.common.model.OrderUserVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 高阶：联表分页。MP 会基于 IPage 自动生成 count 语句，
     * wrapper 通过 ${ew.customSqlSegment} 拼接到自定义 SQL 上（含分页 limit）。
     */
    @Select("SELECT u.name AS userName, o.amount, o.create_time AS orderTime " +
            "FROM t_order o LEFT JOIN t_user u ON o.user_id = u.id ${ew.customSqlSegment}")
    IPage<OrderUserVO> selectOrderUserPage(IPage<OrderUserVO> page,
                                           @Param(Constants.WRAPPER) Wrapper<OrderUserVO> wrapper);
}
