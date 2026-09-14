package lan.chaos.microservice.user.service;

import lan.chaos.microservice.user.entity.UserTag;

import java.util.List;

public interface UserTagService {

    /** 给用户加标签（副库 MySQL） */
    UserTag addTag(Long userId, String tag);

    /** 查某用户的全部标签（副库 MySQL） */
    List<UserTag> listTags(Long userId);
}
