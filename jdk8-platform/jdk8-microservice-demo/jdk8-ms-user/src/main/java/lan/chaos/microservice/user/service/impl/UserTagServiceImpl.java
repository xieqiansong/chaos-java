package lan.chaos.microservice.user.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import lan.chaos.microservice.common.core.constant.DataSourceConstants;
import lan.chaos.microservice.user.entity.UserTag;
import lan.chaos.microservice.user.mapper.UserTagMapper;
import lan.chaos.microservice.user.service.UserTagService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户标签服务（副库 MySQL）。
 *
 * <p>关键：{@link DS} 注解把整个类的方法路由到名为 {@code mysql} 的数据源，
 * 与 {@link lan.chaos.microservice.user.service.impl.UserServiceImpl}（走默认 PG）形成对比，
 * 证明“同一事务/同一应用内按注解切库”。注解打在类上即对该类所有方法生效；也可打在方法上做更细粒度切换。</p>
 */
@DS(DataSourceConstants.MYSQL)
@Service
public class UserTagServiceImpl implements UserTagService {

    @Resource
    private UserTagMapper userTagMapper;

    @Override
    public UserTag addTag(Long userId, String tag) {
        UserTag userTag = new UserTag();
        userTag.setUserId(userId);
        userTag.setTag(tag);
        userTagMapper.insert(userTag);
        return userTag;
    }

    @Override
    public List<UserTag> listTags(Long userId) {
        return userTagMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserTag>()
                        .eq(UserTag::getUserId, userId));
    }
}
