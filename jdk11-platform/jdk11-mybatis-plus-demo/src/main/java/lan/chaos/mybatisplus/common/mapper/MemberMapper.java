package lan.chaos.mybatisplus.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lan.chaos.mybatisplus.common.model.Member;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 绕过 TypeHandler 直读数据库原始值（密文），用于验证「落库即密文」。
     */
    @Select("SELECT phone FROM member WHERE id = #{id}")
    String selectRawPhoneById(@Param("id") Long id);
}
