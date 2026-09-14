package lan.chaos.microservice.user.service.impl;

import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.user.entity.User;
import lan.chaos.microservice.user.mapper.UserMapper;
import lan.chaos.microservice.user.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户服务（主库 PG，无 @DS 即走 primary 数据源）。
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User createUser(String username, String nickname, Integer age, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAge(age);
        user.setPhone(phone);
        userMapper.insert(user);
        return user;
    }

    @Override
    public User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return user;
    }

    @Override
    public List<User> listUsers() {
        return userMapper.selectList(null);
    }
}
