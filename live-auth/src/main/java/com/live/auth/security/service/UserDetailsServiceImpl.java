package com.live.auth.security.service;

import com.live.admin.api.UserFeignClient;
import com.live.admin.pojo.entity.SysUser;
import com.live.auth.common.enums.OAuthClientEnum;
import com.live.auth.domain.OAuthUserDetails;
import com.live.common.result.Result;
import com.live.common.result.ResultCode;
import com.live.common.web.util.JwtUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 【重要】从数据库获取用户信息，用于和前端传过来的用户信息进行密码判读
 * @author puff
 * @date 2020-05-27
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private UserFeignClient userFeignClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String clientId = JwtUtils.getOAuthClientId();
        OAuthClientEnum client = OAuthClientEnum.getByClientId(clientId);

        Result result;
        OAuthUserDetails oauthUserDetails = null;
        switch (client) {
            default:
                result = userFeignClient.getUserByUsername(username);
                if (ResultCode.SUCCESS.getCode().equals(result.getCode())) {
                    SysUser sysUser = (SysUser)result.getData();
                    oauthUserDetails = new OAuthUserDetails(sysUser);
                }
                break;
        }
        if (oauthUserDetails == null || oauthUserDetails.getId() == null) {
            throw new UsernameNotFoundException(ResultCode.USER_NOT_EXIST.getMsg());
        } else if (!oauthUserDetails.isEnabled()) {
            throw new DisabledException("该账户已被禁用!");
        } else if (!oauthUserDetails.isAccountNonLocked()) {
            throw new LockedException("该账号已被锁定!");
        } else if (!oauthUserDetails.isAccountNonExpired()) {
            throw new AccountExpiredException("该账号已过期!");
        }
        return oauthUserDetails;
    }

}
