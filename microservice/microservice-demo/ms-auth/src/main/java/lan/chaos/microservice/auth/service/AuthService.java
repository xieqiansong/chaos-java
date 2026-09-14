package lan.chaos.microservice.auth.service;

import lan.chaos.microservice.auth.model.LoginRequest;
import lan.chaos.microservice.auth.model.LoginResponse;

/**
 * 鉴权服务（登录/刷新/登出）。
 */
public interface AuthService {

    /** 用户名+密码登录，签发双令牌。 */
    LoginResponse login(LoginRequest request);

    /** 用刷新令牌换取新的访问令牌。 */
    LoginResponse refresh(String refreshToken);

    /** 登出：吊销该用户全部刷新令牌（踢下线）。 */
    void logout(Long userId);
}
