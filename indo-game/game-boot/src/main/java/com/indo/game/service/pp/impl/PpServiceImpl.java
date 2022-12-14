package com.indo.game.service.pp.impl;


import com.alibaba.fastjson.JSONObject;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.redis.utils.GeneratorIdUtil;
import com.indo.common.result.Result;
import com.indo.common.utils.DateUtils;
import com.indo.common.utils.GameUtil;
import com.indo.game.common.util.PPHashAESEncrypt;
import com.indo.core.mapper.game.TxnsMapper;
import com.indo.game.pojo.dto.comm.ApiResponseData;
import com.indo.game.pojo.dto.pp.PpApiGetBalanceReq;
import com.indo.game.pojo.dto.pp.PpApiRequestData;
import com.indo.game.pojo.dto.pp.PpApiResponseData;
import com.indo.game.pojo.dto.pp.PpApiStartGameReq;
import com.indo.game.pojo.dto.pp.PpApiTransferReq;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.core.pojo.entity.game.GameCategory;
import com.indo.core.pojo.entity.game.GameParentPlatform;
import com.indo.core.pojo.entity.game.GamePlatform;
import com.indo.core.pojo.entity.game.Txns;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.pp.PpService;
import com.indo.core.pojo.bo.MemTradingBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Service
public class PpServiceImpl implements PpService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private CptOpenMemberService externalService;
    @Autowired
    private GameCommonService gameCommonService;
    @Autowired
    private TxnsMapper txnsMapper;

    @Override
    public Result ppGame(LoginInfo loginUser, String isMobileLogin, String ip, String platform, String parentName) {
        logger.info("pplog ppGame account:{},ppCodeId:{}", loginUser.getId(), loginUser.getAccount(), platform);
        // ??????????????????
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(parentName);
        if (null == gameParentPlatform) {
            return Result.failed("(" + parentName + ")???????????????");
        }
        if (0==gameParentPlatform.getIsStart()) {
            return Result.failed("g100101", "???????????????");
        }
        if ("1".equals(gameParentPlatform.getIsOpenMaintenance())) {
            return Result.failed("g000001", gameParentPlatform.getMaintenanceContent());
        }
        // ??????????????????
        GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(platform,parentName);
        if (null == gamePlatform) {
            return Result.failed("(" + platform + ")???????????????");
        }
        if (0==gamePlatform.getIsStart()) {
            return Result.failed("g100102", "???????????????");
        }
        if ("1".equals(gamePlatform.getIsOpenMaintenance())) {
            return Result.failed("g091047", gamePlatform.getMaintenanceContent());
        }

        BigDecimal balance = loginUser.getBalance();
        //??????????????????
        if (null == balance || balance.compareTo(BigDecimal.ZERO) == 0) {
            logger.info("??????pp???????????????????????????memid {},nickName {},balance {}", loginUser.getId(), loginUser.getNickName(), balance);
            //????????????????????????
            return Result.failed("g300004", "??????????????????");
        }

        try {

            // ??????????????????KY-CPT????????????????????????
            CptOpenMember cptOpenMember = externalService.getCptOpenMember(loginUser.getId().intValue(), parentName);
            if (cptOpenMember == null) {
                cptOpenMember = new CptOpenMember();
                cptOpenMember.setUserId(loginUser.getId().intValue());
                cptOpenMember.setUserName(loginUser.getAccount());
                cptOpenMember.setPassword(GeneratorIdUtil.generateId());
                cptOpenMember.setCreateTime(new Date());
                cptOpenMember.setLoginTime(new Date());
                cptOpenMember.setType(parentName);
                //????????????
                createMemberGame(cptOpenMember);
            } else {
                cptOpenMember.setLoginTime(new Date());
                externalService.updateCptOpenMember(cptOpenMember);

                // ????????????
                loginOutPP(loginUser);
            }

            PpApiStartGameReq ppApiRequestData = new PpApiStartGameReq();
            ppApiRequestData.setSecureLogin(OpenAPIProperties.PP_SECURE_LOGIN);
            ppApiRequestData.setExternalPlayerId(loginUser.getAccount());
            ppApiRequestData.setGameId(platform);
            ppApiRequestData.setLanguage(gameParentPlatform.getLanguageType());
            return startGame(ppApiRequestData, ip);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("g100104", "?????????????????????????????????");
        }
    }

    @Override
    public Result logout(LoginInfo loginUser, String platform, String ip) {
        logger.info("pplogout ppGame account:{},t9CodeId:{}", loginUser.getId(), loginUser.getAccount(), platform);
        try {
            // ????????????
            PpApiResponseData ppCommonResp = loginOutPP(loginUser);

            if (0 == ppCommonResp.getError()) {
                return Result.success(ppCommonResp);
            } else {
                return errorCode(ppCommonResp.getError().toString(), ppCommonResp.getDescription());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("g100104", "?????????????????????????????????");
        }
    }

    private PpApiResponseData loginOutPP(LoginInfo loginUser) throws Exception {
        PpApiRequestData ppApiRequestData = new PpApiRequestData();
        ppApiRequestData.setSecureLogin(OpenAPIProperties.PP_SECURE_LOGIN);
        ppApiRequestData.setExternalPlayerId(loginUser.getAccount());

        // ??????????????????
        Map<String, Object> params = getPostParams(ppApiRequestData);

        // ????????????
        return commonRequest(getLogOutPpPlayerUrl(), params, loginUser.getId(), "loginoutPP");
    }

    @Override
    public Result transfer(PpApiTransferReq ppApiTransferReq, String ip) {
        logger.info("pp_transfer ppGame paramJson:{}, ip:{}", JSONObject.toJSONString(ppApiTransferReq), ip);
        try {
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(ppApiTransferReq.getExternalPlayerId());
            GamePlatform gamePlatform = gameCommonService.getGamePlatformByParentName(OpenAPIProperties.PP_PLATFORM_CODE).get(0);
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
            GameParentPlatform platformGameParent = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PP_PLATFORM_CODE);

            // ????????????
            BigDecimal balance = memBaseinfo.getBalance();
            // ????????????
            BigDecimal betAmount = ppApiTransferReq.getAmount();
            if (memBaseinfo.getBalance().compareTo(betAmount) < 0) {
                return Result.failed("g300004", "??????????????????");
            }
            // ??????????????????ID
            String transactionId = GoldchangeEnum.DSFYXZZ.name() + GeneratorIdUtil.generateId();
            ppApiTransferReq.setExternalTransactionId(transactionId);

            // ???????????????
            PpApiRequestData ppApiRequestData = new PpApiRequestData();
            ppApiRequestData.setSecureLogin(OpenAPIProperties.PP_SECURE_LOGIN);
            ppApiRequestData.setExternalPlayerId(memBaseinfo.getAccount());
            ppApiRequestData.setExternalTransactionId(transactionId);
            ppApiRequestData.setAmount(betAmount);

            // ??????????????????
            Map<String, Object> params = getPostParams(ppApiRequestData);

            // ??????PP
            PpApiResponseData ppApiResponseData = commonRequest(
                    getTransferUrl(), params,
                    memBaseinfo.getId(), "transferPP");

            if (0 != ppApiResponseData.getError()) {
                return errorCode(ppApiResponseData.getError().toString(), ppApiResponseData.getDescription());
            }
            // ??????0??? ?????????????????? ??????PP??????
            if (betAmount.compareTo(BigDecimal.ZERO) > 0) {

                balance = balance.subtract(betAmount);
                gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.DSFYXZZ, TradingEnum.SPENDING);
            }
            // ??????0??? ???PP??????????????? ????????????
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {

                balance = balance.add(betAmount.abs());
                gameCommonService.updateUserBalance(memBaseinfo, betAmount.abs(), GoldchangeEnum.DSFYXZZ, TradingEnum.SPENDING);
            }

            Txns txns = new Txns();
            //??????????????????
            txns.setPlatformTxId(transactionId);
            //???????????????????????? true????????? false ???
            txns.setBet(false);
            //?????? ID
            txns.setUserId(memBaseinfo.getId().toString());
            //??????????????????
            txns.setCurrency(platformGameParent.getCurrencyType());
            //????????????
            txns.setPlatform(platformGameParent.getPlatformCode());
            //????????????
            txns.setPlatformEnName(platformGameParent.getPlatformEnName());
            txns.setPlatformCnName(platformGameParent.getPlatformCnName());
            //??????????????????
            txns.setGameType(gameCategory.getGameType());
            //????????????ID
            txns.setCategoryId(gameCategory.getId());
            //??????????????????
            txns.setCategoryName(gameCategory.getGameName());
            //??????????????????
            txns.setGameCode(gamePlatform.getPlatformCode());
            //????????????
            txns.setGameName(gamePlatform.getPlatformEnName());
            //????????????
            txns.setBetAmount(ppApiTransferReq.getAmount());
            // PP??????ID
            txns.setRePlatformTxId(ppApiResponseData.getTransactionId());
            //???????????????????????????
//            txns.setBetType(ppApiTransferReq.getGameId());
            //???????????????????????????????????????????????????0??????????????????
//            txns.setWinningAmount(BigDecimal.ZERO);
            //??????????????????
            txns.setBetTime(DateUtils.format(new Date(), DateUtils.newFormat));
            //??????????????????,???????????????????????????
            txns.setRealBetAmount(ppApiTransferReq.getAmount());
            //??????????????????,????????????
            txns.setRealWinAmount(BigDecimal.ZERO);
            //?????????????????? ??? ????????????
            txns.setTurnover(ppApiTransferReq.getAmount());
            //????????????????????????
            txns.setTxTime(DateUtils.format(new Date(), DateUtils.newFormat));
            //????????????
            txns.setMethod("Settle");
            txns.setStatus("Running");
            //??????
            txns.setBalance(balance);
            //????????????
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            txns.setCreateTime(dateStr);
            //?????? IP
            txns.setBetIp(ip);//  string ??? ?????? IP
            int num = txnsMapper.insert(txns);
            if (num <= 0) {
                int count = 0;
                // ????????????
                while (count < 5) {
                    num = txnsMapper.insert(txns);
                    if (num > 0) break;
                    count++;
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.failed("g100104", "?????????????????????????????????");
        }

        return Result.success();
    }

    @Override
    public Result getBalance(PpApiGetBalanceReq ppApiGetBalanceReq, String ip) {
        logger.info("pp_getBalance ppGame paramJson:{}, ip:{}", JSONObject.toJSONString(ppApiGetBalanceReq), ip);
        PpApiResponseData ppApiResponseData;
        try {
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(ppApiGetBalanceReq.getExternalPlayerId());

            if (null == memBaseinfo) {
                return Result.failed("g010001", "?????????????????????");
            }

            // ???????????????
            PpApiRequestData ppApiRequestData = new PpApiRequestData();
            ppApiRequestData.setSecureLogin(OpenAPIProperties.PP_SECURE_LOGIN);
            ppApiRequestData.setExternalPlayerId(memBaseinfo.getAccount());

            Map<String, Object> params = getPostParams(ppApiRequestData);

            // ????????????
            ppApiResponseData = commonRequest(
                    getBalanceUrl(), params,
                    memBaseinfo.getId(), "getBalancePP");

            if (0 != ppApiResponseData.getError()) {
                return errorCode(ppApiResponseData.getError().toString(), ppApiResponseData.getDescription());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.failed("g100104", "?????????????????????????????????");
        }

        return Result.success(JSONObject.toJSONString(ppApiResponseData));
    }

    @Override
    public Result startGame(PpApiStartGameReq ppApiStartGameReq, String ip) {
        logger.info("pp_startGame ppGame paramJson:{}, ip:{}", JSONObject.toJSONString(ppApiStartGameReq), ip);
        PpApiResponseData ppApiResponseData;
        try {
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(ppApiStartGameReq.getExternalPlayerId());

            if (null == memBaseinfo) {
                return Result.failed("g010001", "?????????????????????");
            }

            // ???????????????
            ppApiStartGameReq.setSecureLogin(OpenAPIProperties.PP_SECURE_LOGIN);
            Map<String, Object> params = getPostParams(ppApiStartGameReq);

            // ??????API
            ppApiResponseData = commonRequest(
                    getStartGameUrl(), params,
                    memBaseinfo.getId(), "startGamePP");

            if (0 != ppApiResponseData.getError()) {
                return errorCode(ppApiResponseData.getError().toString(), ppApiResponseData.getDescription());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.failed("g100104", "?????????????????????????????????");
        }

        ApiResponseData responseData = new ApiResponseData();
        responseData.setPathUrl(ppApiResponseData.getGameURL());
        return Result.success(responseData);
    }

    /**
     * ?????????????????????map, ????????????
     *
     * @param obj
     * @return
     */
    private Map<String, Object> getPostParams(Object obj) {
        Map<String, Object> params = objectToMap(obj);
        params.put("hash", PPHashAESEncrypt.encrypt(obj, OpenAPIProperties.PP_API_SECRET_KEY));
        return params;
    }

    /**
     * ?????????MAP
     *
     * @param obj
     * @return
     */
    private Map<String, Object> objectToMap(Object obj) {
        String json = JSONObject.toJSONString(obj);
        return JSONObject.parseObject(json);
    }

    /**
     * ??????????????????
     *
     * @param cptOpenMember
     * @return
     */
    private Result createMemberGame(CptOpenMember cptOpenMember) {
        // ??????PP??????
        PpApiResponseData ppApiResponseData = createPpMember(cptOpenMember);
        if (null == ppApiResponseData) {
            return Result.failed("g091087", "????????????????????????");
        }

        if (0 == ppApiResponseData.getError()) {
            cptOpenMember.setUserName(ppApiResponseData.getPlayerId());
            externalService.saveCptOpenMember(cptOpenMember);
            return Result.success();
        } else {
            return errorCode(ppApiResponseData.getError().toString(), ppApiResponseData.getDescription());
        }
    }

    /**
     * ??????PP??????
     *
     * @param cptOpenMember???
     * @return
     */
    private PpApiResponseData createPpMember(CptOpenMember cptOpenMember) {
        GameParentPlatform platformGameParent = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PP_PLATFORM_CODE);
        PpApiRequestData ppApiRequestData = new PpApiRequestData();
        ppApiRequestData.setSecureLogin(OpenAPIProperties.PP_SECURE_LOGIN);
        ppApiRequestData.setExternalPlayerId(cptOpenMember.getUserName());
        ppApiRequestData.setCurrency(platformGameParent.getCurrencyType());

        Map<String, Object> params = getPostParams(ppApiRequestData);

        PpApiResponseData ppApiResponseData = null;
        try {
            ppApiResponseData = commonRequest(
                    getCreatePpPlayerUrl(), params,
                    cptOpenMember.getUserId(), "createPpPlayer");
        } catch (Exception e) {
            logger.error("pplog createPpMember:{}", e);
            e.printStackTrace();
        }
        return ppApiResponseData;

    }

    /**
     * ????????????
     */
    public PpApiResponseData commonRequest(String apiUrl, Map<String, Object> params, Object userId, String method) throws Exception {
        logger.info("pplog commonRequest userId:{},paramsMap:{}", userId, params);
        String result = GameUtil.postForm4PP(apiUrl, params, method);
        PpApiResponseData response = JSONObject.parseObject(result, PpApiResponseData.class);
        return response;
    }

    /**
     * ????????????API??????
     *
     * @return
     */
    private String getCreatePpPlayerUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append(OpenAPIProperties.PP_API_URL).append("/player/account/create/");
        return builder.toString();
    }

    /**
     * ??????????????????API??????
     *
     * @return
     */
    private String getLogOutPpPlayerUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append(OpenAPIProperties.PP_API_URL).append("/game/session/terminate/");
        return builder.toString();
    }

    /**
     * ????????????
     *
     * @return
     */
    private String getBalanceUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append(OpenAPIProperties.PP_API_URL).append("/balance/current/");
        return builder.toString();
    }

    /**
     * ??????
     *
     * @return
     */
    private String getTransferUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append(OpenAPIProperties.PP_API_URL).append("/balance/transfer");
        return builder.toString();
    }

    /**
     * ????????????
     *
     * @return
     */
    private String getStartGameUrl() {
        StringBuilder builder = new StringBuilder();
        builder.append(OpenAPIProperties.PP_API_URL).append("/game/start/");
        return builder.toString();
    }


    public Result errorCode(String errorCode, String errorMessage) {
//        200 ?????????                                                Succeed.
        switch (errorCode) {
//        1 ????????????????????????
            case "1":
                return Result.failed("g100104", errorMessage);
//        100 {????????????}???????????????????????? ???GetTransferStatus ?????????           No authorized to access
            case "100":
                return Result.failed("g100104", errorMessage);

//        2 ??????????????????????????????????????????                            Domain is null or the length of domain less than 2.
            case "2":
                return Result.failed("g100107", errorMessage);

//        6 ???????????????????????????????????????                                        Failed to pass the domain validation.
            case "6":
                return Result.failed("g100102", errorMessage);

//        7??????????????????????????????????????????????????????   The encrypted data is null or the length of the encrypted data is equal to 0.
            case "7":
                return Result.failed("g000007", errorMessage);

//        8 ???????????????            Assertion(SAML) didn't pass the timestamp validation.
            case "8":
                return Result.failed("g091016", errorMessage);

//        17 ??????????????????                      Failed to extract the SAML parameters from the encrypted data.
            case "17":
                return Result.failed("g010001", errorMessage);

//        21 ????????????????????????????????????                                            Unknow action.
            case "21":
                return Result.failed("g100001", errorMessage);
//        9999 ?????????                                                Failed.
            default:
                return Result.failed("g009999", errorMessage);
        }
    }
}
