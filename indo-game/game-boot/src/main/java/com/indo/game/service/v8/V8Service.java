package com.indo.game.service.v8;

import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.result.Result;

import java.math.BigDecimal;

public interface V8Service {

    /**
     * 登录游戏V8
     */
    Result v8Game(LoginInfo loginUser, String isMobileLogin, String ip, String platform, String parentName);

    /**
     * V8电子游戏 强迫登出玩家
     */
    Result logout(LoginInfo loginUser,String platform, String ip);

    /**
     * V8游戏下分
     * @param loginUser loginUser
     * @param platform platform
     * @param money money
     * @param ip ip
     * @return Result
     */
    Result crebit(LoginInfo loginUser, String platform, BigDecimal money, String ip);

    /**
     * V8游戏玩家总分
     * @param loginUser loginUser
     * @param platform platform
     * @param ip ip
     * @return Result
     */
    Result balance(LoginInfo loginUser, String platform, String ip);
}
