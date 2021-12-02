package com.indo.game.service.awc.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.result.Result;
import com.indo.common.utils.DateUtils;
import com.indo.common.utils.i18n.MessageUtils;
import com.indo.game.common.constant.Constants;
import com.indo.game.config.OpenAPIProperties;
import com.indo.game.game.RedisBaseUtil;
import com.indo.game.game.RedisLock;
import com.indo.game.mapper.awc.AwcAeSexybcrtTransactionMapper;
import com.indo.game.mapper.manage.GameCategoryMapper;
import com.indo.game.mapper.manage.GameTypeMapper;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.game.pojo.entity.awc.AwcAeSexybcrtTransaction;
import com.indo.game.pojo.entity.awc.AwcApiResponseData;
import com.indo.game.pojo.entity.manage.GameCategory;
import com.indo.game.pojo.entity.manage.GamePlatform;
import com.indo.game.pojo.entity.manage.GameType;
import com.indo.game.service.awc.AwcService;
import com.indo.game.common.util.AWCUtil;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.gamecommon.GameCommonService;
import com.indo.game.service.membaseinfo.MemBaseinfoService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * awc ae真人 游戏业务类
 *
 * @author eric
 */
@Service
public class AwcServiceImpl implements AwcService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private CptOpenMemberService externalService;
    @Autowired
    private GameCommonService gameCommonService;
    @Autowired
    private MemBaseinfoService memBaseinfoService;
    @Autowired
    GameTypeMapper gameTypeMapper;
    @Autowired
    AwcAeSexybcrtTransactionMapper awcAeSexybcrtTransactionMapper;
    @Autowired
    GameCategoryMapper gameCategoryMapper;

    /**
     * 登录游戏AWC-AE真人
     * @param gameCode
     * @return loginUser 用户信息
     */
    @Override
    public Result<String> awcGame(LoginInfo loginUser, String isMobileLogin,String gameCode, String ip,String platform) {
        logger.info("awclog {} aeGame account:{}, aeCodeId:{}", loginUser.getId(), loginUser.getNickName(), gameCode);
        // 是否开售校验
        GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCode(platform);
        if(null==gamePlatform){
            return Result.failed("(awc)"+MessageUtils.get("tgdne"));
        }
        if ("0".equals(gamePlatform.getIsStart())) {
            return Result.failed(MessageUtils.get("tgocinyo"));
        }
        //初次判断站点棋牌余额是否够该用户
        BigDecimal balance = memBaseinfoService.getBalanceById(loginUser.getId().intValue());
        //验证站点棋牌余额
        if (null==balance || BigDecimal.ZERO==balance) {
            logger.info("站点awc余额不足，当前用户memid {},nickName {},balance {}", loginUser.getId(), loginUser.getNickName(), balance);
            //站点棋牌余额不足
            return Result.failed(MessageUtils.get("tcgqifpccs"));
        }

        String initKey = "AWC_AESEXYBCRT_GAME_LOGIN_" + loginUser.getId();
        RedisLock lock = new RedisLock(initKey, 0, Constants.AE_TIMEOUT_MSECS);
        try {
            if (lock.lock()) {
                String key = Constants.AWC_AESEXYBCRT_ACCOUNT_TYPE + "_" + loginUser.getId();
                long total = RedisBaseUtil.increment(key, 1);
                RedisBaseUtil.setExpire(key, 3, TimeUnit.SECONDS);
                if (total > 1) {
                    logger.error("awclog cyCallback[{}] ", loginUser.getId());
                    return Result.failed(MessageUtils.get("frequentoperation"));
                }

                // 验证且绑定（KY-CPT第三方会员关系）
                CptOpenMember cptOpenMember = externalService.getCptOpenMember(loginUser.getId().intValue(), Constants.AWC_AESEXYBCRT_ACCOUNT_TYPE);
                if (cptOpenMember == null) {
                    cptOpenMember = new CptOpenMember();
                    cptOpenMember.setUserId(loginUser.getId().intValue());
                    cptOpenMember.setUsername(loginUser.getAccount());
                    cptOpenMember.setPassword(loginUser.getAccount());
                    cptOpenMember.setCreateTime(new Date());
                    cptOpenMember.setLoginTime(new Date());
                    cptOpenMember.setType(Constants.AWC_AESEXYBCRT_ACCOUNT_TYPE);
                    //创建玩家
                    return createMemberGame(gamePlatform, ip, cptOpenMember);
                } else {
                    CptOpenMember updateCptOpenMember = new CptOpenMember();
                    updateCptOpenMember.setId(cptOpenMember.getId());
                    updateCptOpenMember.setLoginTime(new Date());
                    externalService.updateCptOpenMember(updateCptOpenMember);
                }
                //登录
                return initGame(gamePlatform, ip, cptOpenMember,isMobileLogin,gameCode);
            } else {
                return Result.failed(MessageUtils.get("etgptal"));
            }
        } catch (Exception e) {
            return Result.failed(MessageUtils.get("tnibptal"));
        } finally {
            lock.unlock();
        }
    }

    /**
     * 登录
     */
    private Result initGame(GamePlatform gamePlatform, String ip, CptOpenMember cptOpenMember,String isMobileLogin,String gameCode) throws Exception {
        AwcApiResponseData result = game(gamePlatform, ip, cptOpenMember,isMobileLogin,gameCode);
        if (null == result ) {
            return Result.failed(MessageUtils.get("etgptal"));
        }
        if("0000".equals(result.getStatus())){
            return Result.success(result);
        }else {
            if("cn".equals(gamePlatform.getLanguageType())){
                return Result.failed(result.getCodeCnMsg());
            }else {
                return Result.failed(result.getCodeEnMsg());
            }
        }
    }
    /**
     * 创建玩家
     */
    private Result createMemberGame(GamePlatform gamePlatform, String ip, CptOpenMember cptOpenMember) throws Exception {
        AwcApiResponseData result = createMember(gamePlatform, ip, cptOpenMember);
        if (null == result ) {
            return Result.failed(MessageUtils.get("etgptal"));
        }
        if("0000".equals(result.getStatus())){
            externalService.saveCptOpenMember(cptOpenMember);
            return Result.success(result);
        }else {
            if("cn".equals(gamePlatform.getLanguageType())){
                return Result.failed(result.getCodeCnMsg());
            }else {
                return Result.failed(result.getCodeEnMsg());
            }
        }
    }

    /***
     * 创建玩家
     * @param gamePlatform
     * @param ip
     * @param cptOpenMember
     * @return
     */
    public AwcApiResponseData createMember(GamePlatform gamePlatform, String ip, CptOpenMember cptOpenMember) {
        try {
            String time = System.currentTimeMillis() / 1000 + "";
            Map<String, String> trr = new HashMap<>();
            trr.put("userId", String.valueOf(cptOpenMember.getUserId()));
            trr.put("currency", "");//玩家货币代码
            trr.put("language", gamePlatform.getLanguageType());
            trr.put("userName", "");//玩家名称
//           platform: SEXYBCRT
//                   - gameType: LIVE
//                   - value (ID): {"limitId":[IDs]}
//            betLimit: {"SEXYBCRT":{"LIVE":{"limitId":[110901,110902]}}}
//            ※Each player allowed max 6 betLimit IDs.
//            ※每个玩家每个最多允许 6 组下注限红 ID
            trr.put("betLimit", "{\""+gamePlatform.getPlatformEnName()+"\":{\"LIVE\":{\"limitId\":["+gamePlatform.getMinBetLimit()+","+gamePlatform.getMaxBetLimit()+"]}}}");//下注限红

            return commonRequest(trr, OpenAPIProperties.AWC_API_URL_LOGIN, cptOpenMember.getUserId(), ip, "createMember");
        } catch (Exception e) {
            logger.error("awclog game error {} ", e);
            return null;
        }
    }

    /***
     * 登录请求
     * @param gamePlatform
     * @param ip
     * @param cptOpenMember
     * @return
     */
    public AwcApiResponseData game(GamePlatform gamePlatform, String ip, CptOpenMember cptOpenMember,String isMobileLogin,String gameCode) {
        try {
            String time = System.currentTimeMillis() / 1000 + "";
            Map<String, String> trr = new HashMap<>();
            trr.put("userId", String.valueOf(cptOpenMember.getUserId()));
//            true 行动设备登入
            if("1".equals(isMobileLogin)){
                trr.put("isMobileLogin", "true");
            }else {
//            false 桌面设备登入
                trr.put("isMobileLogin", "false");
            }
//            用于导回您指定的网站，需要设置 http:// 或 https://
//            Example 范例：http://www.google.com
            trr.put("externalURL", "");
            trr.put("platform", gamePlatform.getPlatformEnName());//游戏平台名称

            LambdaQueryWrapper<GameType> wrapper = new LambdaQueryWrapper<GameType>();
            wrapper.eq(GameType::getGameCode,gameCode);
            GameType gameType = gameTypeMapper.selectOne(wrapper);
            GameCategory gameCategory = gameCategoryMapper.selectById(gameType.getCategoryId());
            trr.put("gameType", gameCategory.getGameType());//平台游戏类型
            trr.put("gameCode", gameCode);//平台游戏代码 Example范例: MX-LIVE-001
            trr.put("language", gamePlatform.getLanguageType());
            trr.put("hall", "");//仅针对 SEXYBCRT 进入特殊游戏大厅  Example 范例：SEXY
//           platform: SEXYBCRT
//                   - gameType: LIVE
//                   - value (ID): {"limitId":[IDs]}
//            betLimit: {"SEXYBCRT":{"LIVE":{"limitId":[110901,110902]}}}
//            ※Each player allowed max 6 betLimit IDs.
//            ※每个玩家每个最多允许 6 组下注限红 ID
            trr.put("betLimit", "");//下注限红

            return commonRequest(trr, OpenAPIProperties.AWC_API_URL_LOGIN, cptOpenMember.getUserId(), ip, "initGame");
        } catch (Exception e) {
            logger.error("awclog game error {} ", e);
            return null;
        }
    }



    /**
     * 查询订单
     */
    public AwcApiResponseData gameOrderNo(CptOpenMember cptOpenMember, String orderNo, String ip) {
        try {
            Map<String, String> trr = new HashMap<>();
            trr.put("txCode", orderNo);
            logger.info("awclog {} start gameOrderNo orderNo {} balance{}", cptOpenMember.getUserId(), orderNo);
            return commonRequest(trr, OpenAPIProperties.AE_API_URL_CHECK_ORDERSTATUS, cptOpenMember.getUserId().intValue(), ip, "gameOrderNo");
//            logger.info("AWCAESEXYBCRTlog {} end gameOrderNo orderNo {} balance{} result{}", cptOpenMember.getUserId(), orderNo, JSONObject.toJSONString(result));
        } catch (Exception e) {
            logger.error("awclog gameOrderNo error {} orderNo{}", e);
            return null;
        }
    }




    /**
     * 取得玩家余额
     */
    public AwcApiResponseData gameBalance(CptOpenMember cptOpenMember, String ip) {
        try {
            String timestamp = System.currentTimeMillis() / 1000 + "";
            Map<String, String> trr = new HashMap<String, String>();
            trr.put("alluser", "0");
            trr.put("userIds", String.valueOf(cptOpenMember.getUserId()));
            trr.put("isFilterBalance", "");
            logger.info("awclog {} start gameBalance balance{}", cptOpenMember.getUserId());
            return commonRequest(trr, OpenAPIProperties.AWC_API_URL_LOGIN, cptOpenMember.getUserId().intValue(), ip, "gameBalance");
//            logger.info("awclog {} end gameBalance  balance{} result{}", cptOpenMember.getUserId(), JSONObject.toJSONString(result));
        } catch (Exception e) {
            logger.error("awclog gameOrderNo error {} orderNo{}", e);
            return null;
        }
    }



    @Override
    public void awcPullOrder(String platform) {
        try {
            // 设置传入的时间格式
            SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.ISO8601_DATE_FORMAT);
            // 指定一个日期
            // 对 calendar 设置为 date 所定的日期
            Date date = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MINUTE, -3);
            String startTime = dateFormat.format(calendar.getTime());
            Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(date);
            calendar1.add(Calendar.MINUTE, 1);
            String endTime = dateFormat.format(calendar1.getTime());


            commonAwcPullOrder(startTime, endTime,platform);
        } catch (Exception e) {
            logger.error("awclog aePullOrder error", e);
        }

    }

    /**
     * 公共获取记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    private void commonAwcPullOrder(String startTime, String endTime,String platform) {
        long start = System.currentTimeMillis();
        try {
            // 拼接参数
            TreeMap<String, String> trr = new TreeMap<>();
//            查询时间，使用 ISO 8601 格式
//            yyyy-MM-ddThh:mm:ss+|-hh:mm
//            Example 范例：2021-03-26T12:00:00+08:00
            trr.put("timeFrom", startTime);
            trr.put("endTime", endTime);
            trr.put("platform", platform);//游戏平台名称
//            若无带入参数则默认回传数值包含以下：
//            -1 Cancel bet 取消投注
//            1 Settled 已结账
//            2 Void 注单无效
//            9 Invalid 无效交易
            trr.put("status", "");
            trr.put("currency", "");//玩家货币代码
            trr.put("gameType", "");//平台游戏类型  Example 范例：LIVE
            trr.put("gameCode", "");//平台游戏代码 Example 范例：MX-LIVE-001

            // 获取游戏注单
            AwcApiResponseData result = commonRequest(trr, OpenAPIProperties.AWC_API_URL_LOGIN, 0, "127.0.0.1", "commonAePullOrder");
            if (null != result && "0000".equals(result.getStatus())) {
                List<AwcAeSexybcrtTransaction> list = (List<AwcAeSexybcrtTransaction>)result.getTransactions();

                if (null!=list && list.size() > 0) {
                    awcAeSexybcrtTransactionMapper.insertBatch(list);
                }
            }
        } catch (Exception e) {
            logger.error("awclog commonAePullOrder error, startTime:{}, endTime:{}, pageNo:{}, pageSize:{}, retryCount:{}", startTime, endTime, e);

            //retry
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                logger.error("awclog commonAePullOrder retry sleep occur error, startTime:{}, endTime:{}, pageNo:{}, pageSize:{}, retryCount:{}", startTime, endTime, e);
            }
            commonAwcPullOrder(startTime, endTime,platform);
        }

        long end = System.currentTimeMillis();
        logger.info("awclog commonAePullOrder end. startTime:{}, endTime:{}, pageNo:{}, pageSize:{}, totalRecord:{}, used times:{}ms", startTime, endTime, end - start);

    }


    /**
     * 公共请求
     */
    @Override
    public AwcApiResponseData commonRequest(Map<String, String> paramsMap, String url, Integer userId, String ip, String type) throws Exception {
        logger.info("awclog {} commonRequest AE_AES_KEY:{},url:{},paramsMap:{}", userId, url, paramsMap);
        JSONObject sortParams = AWCUtil.sortMap(paramsMap);
        AwcApiResponseData awcApiResponse = null;
        paramsMap.put("cert", OpenAPIProperties.AWC_CERT);
        paramsMap.put("agentId", OpenAPIProperties.AWC_AGENTID);
        String resultString = AWCUtil.doProxyPostJson(OpenAPIProperties.PROXY_HOST_NAME, OpenAPIProperties.PROXY_PORT, "http", url, paramsMap, type, userId);
        if (StringUtils.isNotEmpty(resultString)) {
            awcApiResponse = JSONObject.parseObject(resultString, AwcApiResponseData.class);
            //String operateFlag = (String) redisTemplate.opsForValue().get(Constants.AE_GAME_OPERATE_FLAG + userId);
            logger.info("awclog {}:commonRequest type:{}, operateFlag:{}, url:{}, hostName:{}, params:{}, result:{}, awcApiResponse:{}",
                    //userId, type, operateFlag, url,
                    userId, type, null, url,
                    OpenAPIProperties.PROXY_HOST_NAME, sortParams.toString(), resultString, JSONObject.toJSONString(awcApiResponse));
        }
        return awcApiResponse;
    }

}