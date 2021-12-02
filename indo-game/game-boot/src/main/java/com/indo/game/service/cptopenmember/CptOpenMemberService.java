package com.indo.game.service.cptopenmember;


import com.indo.game.pojo.entity.CptOpenMember;

/**
 * 第三方通用查询用户信息服务接口
 *
 * @author dlucky
 */
public interface CptOpenMemberService {

    /**
     * 根据id 类型 查询第三方用户信息
     *
     * @param userId
     * @param type
     * @return
     */
    CptOpenMember getCptOpenMember(Integer userId, String type);

    void saveCptOpenMember(CptOpenMember cptOpenMember);

    void updateCptOpenMember(CptOpenMember cptOpenMember);

}