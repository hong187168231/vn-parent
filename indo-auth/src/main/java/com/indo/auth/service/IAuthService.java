package com.indo.auth.service;

import com.indo.auth.domain.OAuthToken;
import com.indo.auth.domain.UserInfo;

/**
 * 描述: [类型描述]
 * 创建时间: 2021/6/8
 *
 * @author 
 * @version 1.0.0
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 */
public interface IAuthService {

    OAuthToken login(String code, UserInfo userInfo);
}
