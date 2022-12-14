package com.indo.game.service.jili;

import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.result.Result;

public interface JiliService {
    /**
     * 登录游戏jili
     */
    Result jiliGame(LoginInfo loginUser, String isMobileLogin, String ip, String platform, String parentName);

    /**
     * jili游戏 强迫登出玩家
     */
    Result logout(LoginInfo loginUser,String platform, String ip);
}
