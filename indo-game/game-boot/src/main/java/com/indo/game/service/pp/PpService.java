package com.indo.game.service.pp;

import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.result.Result;
import com.indo.game.pojo.dto.pp.PpApiGetBalanceReq;
import com.indo.game.pojo.dto.pp.PpApiStartGameReq;
import com.indo.game.pojo.dto.pp.PpApiTransferReq;

import java.math.BigDecimal;

public interface PpService {

    /**
     * 登录游戏PP电子
     */
    Result ppGame(LoginInfo loginUser, String isMobileLogin, String ip, String platform, String parentName);

    /**
     * PP电子游戏 强迫登出玩家
     */
    Result logout(LoginInfo loginUser, String platform, String ip);

    /**
     * PP电子充值提款
     */
    Result transfer(PpApiTransferReq ppApiTransferReq, String ip);

    /**
     * 查询PP电子余额
     */
    Result getBalance(PpApiGetBalanceReq ppApiGetBalanceReq, String ip);

    /**
     * 启动PP游戏
     */
    Result startGame(PpApiStartGameReq ppApiStartGameReq, String ip);
}
