package com.live.auth.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.convert.Convert;
import com.live.auth.common.jwt.JwtGenerator;
import com.live.auth.domain.OAuthToken;
import com.live.auth.domain.UserInfo;
import com.live.auth.service.IAuthService;
import com.live.common.constant.AuthConstants;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author puff
 * @description 微信小程序认证接口
 * @createTime 2021/5/20 23:37
 */
@Service
@AllArgsConstructor
public class WechatAuthServiceImpl implements IAuthService {

    private WxMaService wxMaService;
    private JwtGenerator jwtGenerator;


    @SneakyThrows
    @Override
    public OAuthToken login(String code, UserInfo userInfo) {
        // 微信小程序的授权code获取openid
//        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
//        String openid = sessionInfo.getOpenid();
//        Result<UmsMember> result = memberFeignClient.getByOpenid(openid);
//        UmsMember member;
//        if (ResultCode.USER_NOT_EXIST.getCode().equals(result.getCode())) {
//            // 用户不存在，注册成为新用户
//            member = new UmsMember();
//            BeanUtil.copyProperties(userInfo, member);
//            member.setOpenid(openid);
//            Result<Long> addRes = memberFeignClient.add(member);
//            Assert.isTrue(ResultCode.SUCCESS.getCode().equals(addRes.getCode()), "微信用户注册失败");
//            member.setId(addRes.getData()); // 新增后有了会员ID
//        } else {
//            member = result.getData();
//        }

        // 自定义JWT生成
        // 1. JWT授权，一般存放用户的角色标识，用于资源服务器（网关）鉴权
        Set<String> authorities = new HashSet<>();
        // 2. JWT增强，携带用户ID等信息
        Map<String, String> additional = new HashMap<>();
        additional.put(AuthConstants.USER_ID_KEY, Convert.toStr(1));
        String accessToken = jwtGenerator.createAccessToken(authorities, additional);

        OAuthToken token = new OAuthToken().accessToken(accessToken);
        return token;
    }
}
