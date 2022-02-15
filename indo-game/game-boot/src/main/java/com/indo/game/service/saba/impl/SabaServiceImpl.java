package com.indo.game.service.saba.impl;

import com.alibaba.fastjson.JSONObject;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.result.Result;
import com.indo.common.utils.i18n.MessageUtils;
import com.indo.common.utils.GameUtil;
import com.indo.game.mapper.frontend.GameCategoryMapper;
import com.indo.game.mapper.frontend.GamePlatformMapper;
import com.indo.game.mapper.frontend.GameTypeMapper;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.game.pojo.entity.manage.GameParentPlatform;
import com.indo.game.pojo.entity.manage.GamePlatform;
import com.indo.game.pojo.vo.callback.saba.SabaApiResponseData;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.saba.SabaService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * awc ae真人 游戏业务类
 *
 * @author eric
 */
@Service
public class SabaServiceImpl implements SabaService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private CptOpenMemberService externalService;
    @Autowired
    private GameCommonService gameCommonService;

    @Autowired
    GameTypeMapper gameTypeMapper;
    @Autowired
    GameCategoryMapper gameCategoryMapper;
    @Autowired
    private GamePlatformMapper gamePlatformMapper;

    /**
     * 登录游戏
     * @return loginUser 用户信息
     */
    @Override
    public Result sabaGame(LoginInfo loginUser, String ip,String platform,String parentName) {
        logger.info("sabalog {} aeGame account:{}, aeCodeId:{}", loginUser.getId(), loginUser.getNickName());
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(parentName);
        if(null==gameParentPlatform){
            return Result.failed("("+parentName+")游戏平台不存在");
        }
        if ("0".equals(gameParentPlatform.getIsStart())) {
            return Result.failed("g"+"100101","游戏平台未启用");
        }
        if ("1".equals(gameParentPlatform.getIsOpenMaintenance())) {
            return Result.failed("g000001",gameParentPlatform.getMaintenanceContent());
        }
        GamePlatform gamePlatform = null;
        if(!platform.equals(parentName)) {
            gamePlatform = new GamePlatform();
            // 是否开售校验
            gamePlatform = gameCommonService.getGamePlatformByplatformCode(platform);
            if (null == gamePlatform) {
                return Result.failed("("+platform+")平台游戏不存在");
            }
            if ("0".equals(gamePlatform.getIsStart())) {
                return Result.failed("g"+"100102","游戏未启用");
            }
            if ("1".equals(gamePlatform.getIsOpenMaintenance())) {
                return Result.failed("g091047",gamePlatform.getMaintenanceContent());
            }
        }
        //初次判断站点棋牌余额是否够该用户
//        MemBaseinfo memBaseinfo = gameCommonService.getByAccountNo(loginUser.getAccount());
        BigDecimal balance = loginUser.getBalance();
        //验证站点棋牌余额
        if (null==balance || BigDecimal.ZERO==balance) {
            logger.info("站点saba余额不足，当前用户memid {},nickName {},balance {}", loginUser.getId(), loginUser.getNickName(), balance);
            //站点棋牌余额不足
            return Result.failed("g300004","会员余额不足");
        }

        try {

            // 验证且绑定（KY-CPT第三方会员关系）
            CptOpenMember cptOpenMember = externalService.getCptOpenMember(loginUser.getId().intValue(), parentName);
            if (cptOpenMember == null) {
                cptOpenMember = new CptOpenMember();
                cptOpenMember.setUserId(loginUser.getId().intValue());
                cptOpenMember.setUserName(loginUser.getAccount());
                cptOpenMember.setPassword(loginUser.getAccount());
                cptOpenMember.setCreateTime(new Date());
                cptOpenMember.setLoginTime(new Date());
                cptOpenMember.setType(parentName);
                //创建玩家
                return restrictedPlayer(gameParentPlatform,loginUser,gamePlatform, ip, cptOpenMember);
            } else {
                CptOpenMember updateCptOpenMember = new CptOpenMember();
                updateCptOpenMember.setId(cptOpenMember.getId());
                updateCptOpenMember.setLoginTime(new Date());
                externalService.updateCptOpenMember(updateCptOpenMember);
                //登录
                return initGame(gameParentPlatform,loginUser,gamePlatform, ip);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("g100104","网络繁忙，请稍后重试！");
        }
    }

    /**
     * 注册会员
     * @return loginUser 用户信息
     */
    public Result restrictedPlayer(GameParentPlatform gameParentPlatform,LoginInfo loginUser,GamePlatform gamePlatform, String ip,CptOpenMember cptOpenMember) {
        logger.info("sabalog {} sabaGame account:{}, sabaCodeId:{}", loginUser.getId(), loginUser.getNickName());
        try {
            Map<String, String> trr = new HashMap<>();
            trr.put("vendor_Member_ID", loginUser.getAccount());//厂商会员识别码（建议跟 Username 一样）, 支援 ASCII Table 33-126, 最大长度 = 30
            trr.put("username", loginUser.getAccount());
            trr.put("oddsType", "");//为此会员设置赔率类型。请参考附件"赔率类型表"
            trr.put("currency", gameParentPlatform.getLanguageType());//为此会员设置币别。请参考附件中"币别表"
            if(null!=gamePlatform) {
                trr.put("maxTransfer", String.valueOf(gamePlatform.getMaxTransfer()));//于 Sportsbook 系统与厂商间的最大限制转帐金额
                trr.put("minTransfer", String.valueOf(gamePlatform.getMinTransfer()));//于 Sportsbook 系统与厂商间的最小限制转帐金额
            }
            SabaApiResponseData sabaApiResponse = commonRequest(trr, OpenAPIProperties.SABA_API_URL+"/api/CreateMember", loginUser.getId().intValue(), ip, "restrictedPlayer");

            if (null == sabaApiResponse ) {
                return Result.failed(MessageUtils.get("etgptal"));
            }
            if("0".equals(sabaApiResponse.getError_code())||"4103".equals(sabaApiResponse.getError_code())){
                externalService.saveCptOpenMember(cptOpenMember);
                return initGame(gameParentPlatform,loginUser,gamePlatform, ip);
            }else {
                return Result.failed(sabaApiResponse.getError_code(),sabaApiResponse.getMessage());
            }
        } catch (Exception e) {
            logger.error("sabalog game error {} ", e);
            return null;
        }
    }

    /**
     * 登录
     */
    private Result initGame(GameParentPlatform gameParentPlatform,LoginInfo loginUser,GamePlatform gamePlatform, String ip) throws Exception {
        logger.info("sabalog {} sabaGame account:{}, sabaCodeId:{}", loginUser.getId(), loginUser.getNickName());
        try {
            Map<String, String> trr = new HashMap<>();
            trr.put("username", loginUser.getAccount());
            trr.put("portfolio", gameParentPlatform.getPlatformCode());

            SabaApiResponseData sabaApiResponse = commonRequest(trr, OpenAPIProperties.SABA_API_URL+"/api/GetSabaUrl", loginUser.getId().intValue(), ip, "restrictedPlayer");
            if("0".equals(sabaApiResponse.getError_code())){
                return Result.success(sabaApiResponse);
            }else {
                return Result.failed(sabaApiResponse.getError_code(),sabaApiResponse.getMessage());
            }
        } catch (Exception e) {
            logger.error("sabalog game error {} ", e);
            return null;
        }
    }

    /**
     * 强迫登出玩家
     */
    public Result logout(LoginInfo loginUser,String ip){
        Map<String, String> trr = new HashMap<>();
        trr.put("Username", loginUser.getAccount());

        SabaApiResponseData sabaApiResponse = null;
        try {
            sabaApiResponse = commonRequest(trr, OpenAPIProperties.SABA_API_URL+"/api/KickUser", Integer.valueOf(loginUser.getId().intValue()), ip, "logout");
            if (null == sabaApiResponse ) {
                return Result.failed(MessageUtils.get("etgptal"));
            }
            if("0".equals(sabaApiResponse.getError_code())){
                return Result.success(sabaApiResponse);
            }else {
                return Result.failed(sabaApiResponse.getError_code(),sabaApiResponse.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("g100104","网络繁忙，请稍后重试！");
        }

    }

    /**
     * 公共请求
     */
    @Override
    public SabaApiResponseData commonRequest(Map<String, String> paramsMap, String url, Integer userId, String ip, String type) throws Exception {
        logger.info("sabalog {} commonRequest ,url:{},paramsMap:{}", userId, url, paramsMap);

        SabaApiResponseData sabaApiResponse = null;
        paramsMap.put("vendor_id", OpenAPIProperties.SABA_SITENAME);
        paramsMap.put("operatorId", OpenAPIProperties.SABA_VENDORID);
        JSONObject sortParams = GameUtil.sortMap(paramsMap);
        Map<String, String> trr = new HashMap<>();
        trr.put("param", sortParams.toString());
        logger.info("ug_api_request:"+sortParams);
        String resultString = GameUtil.doProxyPostJson(OpenAPIProperties.PROXY_HOST_NAME, OpenAPIProperties.PROXY_PORT, OpenAPIProperties.PROXY_TCP,url, trr, type, userId);
        logger.info("saba_api_response:"+resultString);
        if (StringUtils.isNotEmpty(resultString)) {
            sabaApiResponse = JSONObject.parseObject(resultString, SabaApiResponseData.class);
            //String operateFlag = (String) redisTemplate.opsForValue().get(Constants.AE_GAME_OPERATE_FLAG + userId);
            logger.info("sabalog {}:commonRequest type:{}, operateFlag:{}, url:{}, hostName:{}, params:{}, result:{}, sabaApiResponse:{}",
                    //userId, type, operateFlag, url,
                    userId, type, null, url, sortParams.toString(), resultString, JSONObject.toJSONString(sabaApiResponse));
        }
        return sabaApiResponse;
    }

}
