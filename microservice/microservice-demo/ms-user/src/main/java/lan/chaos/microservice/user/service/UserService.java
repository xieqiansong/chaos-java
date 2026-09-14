package lan.chaos.microservice.user.service;

import lan.chaos.microservice.user.entity.User;

import java.util.List;

public interface UserService {

    /** 创建用户（主库 PG） */
    User createUser(String username, String nickname, Integer age, String phone);

    /** 按 id 查询；不存在抛 BizException(NOT_FOUND) */
    User getUser(Long id);

    /** 列表查询 */
    List<User> listUsers();
}
