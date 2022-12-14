package com.indo.game.service.redtiger.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.utils.DateUtils;
import com.indo.core.mapper.game.TxnsMapper;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.core.pojo.entity.game.GameCategory;
import com.indo.core.pojo.entity.game.GameParentPlatform;
import com.indo.core.pojo.entity.game.GamePlatform;
import com.indo.core.pojo.entity.game.Txns;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.redtiger.RedtigerCallbackService;
import com.indo.core.pojo.bo.MemTradingBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Service
public class RedtigerCallbackServiceImpl implements RedtigerCallbackService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private CptOpenMemberService externalService;
    @Resource
    private GameCommonService gameCommonService;

    @Autowired
    private TxnsMapper txnsMapper;
    @Override
    public Object sid(JSONObject params,String authToken ,String ip) {
        logger.info("redtiger_check redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        String sid = params.getString("sid");
        jsonObject.put("sid",sid);
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        try {


            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }

            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }
            // ????????????????????????
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }

            if (null == sid || "".equals(sid)) {
                CptOpenMember cptOpenMember = externalService.getCptOpenMember(playerID, OpenAPIProperties.REDTIGER_PLATFORM_CODE);
                if(null!=cptOpenMember) {
                    sid = cptOpenMember.getPassword();
                }else {
                    sid = uuid;
                }
            }
            jsonObject.put("sid",sid);
            jsonObject.put("uuid", uuid);

            return jsonObject;

        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    @Override
    public Object check(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_check redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        String sid = params.getString("sid");
        jsonObject.put("sid",sid);
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }

            // ????????????????????????
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }


            return jsonObject;

        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    @Override
    public Object balance(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_balance redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }

            // ????????????????????????
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }

            // ??????????????????
            jsonObject.put("balance", memBaseinfo.getBalance().divide(platformGameParent.getCurrencyPro()));
            jsonObject.put("bonus", 0);
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("balance", BigDecimal.ZERO);
            jsonObject.put("bonus", 0);
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    @Override
    public Object debit(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_debit  redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }

//            JSONObject params = JSONObject.parseObject(String.valueOf(map));
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }

            JSONObject transaction = params.getJSONObject("transaction");
            String platformTxId = transaction.getString("id");
            String refId = transaction.getString("refId");
            JSONObject game = params.getJSONObject("game");
            JSONObject table = game.getJSONObject("details").getJSONObject("table");

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }
            GamePlatform gamePlatform;
            if("Y".equals(OpenAPIProperties.REDTIGER_IS_PLATFORM_LOGIN)){
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(platformGameParent.getPlatformCode(),platformGameParent.getPlatformCode());
            }else {
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(table.getString("id"), platformGameParent.getPlatformCode());

            }
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());

            // ????????????
            BigDecimal balance = memBaseinfo.getBalance();
            // ????????????
            BigDecimal betAmount = null!=transaction.getBigDecimal("amount")?transaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;
            if (memBaseinfo.getBalance().compareTo(betAmount) < 0) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INSUFFICIENT_FUNDS");
                return jsonObject;
            }

            // ????????????????????????
            Txns oldTxns = getTxnsByRoundId(refId, memBaseinfo.getAccount());
            if (null != oldTxns&&"Place Bet".equals(oldTxns.getMethod())) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
//                jsonObject.put("status", "BET_ALREADY_SETTLED");
                return jsonObject;
            }else if (null != oldTxns){
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "FINAL_ERROR_ACTION_FAILED");
                return jsonObject;
            }

            // ??????????????????0
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INSUFFICIENT_FUNDS");
                return jsonObject;
            }
            balance = balance.subtract(betAmount);
            // ??????????????????
            gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);

            Txns txns = new Txns();
            //??????????????????
            txns.setPlatformTxId(platformTxId);
            //???????????????????????? true????????? false ???
            txns.setBet(true);
            //?????? ID
            txns.setUserId(memBaseinfo.getAccount());
            //??????????????????
            txns.setCurrency(platformGameParent.getCurrencyType());
            txns.setGameInfo(game.getString("type"));
            txns.setRoundId(refId);
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
            txns.setBetAmount(betAmount);
            //???????????????????????????????????????????????????0??????????????????
            txns.setWinningAmount(betAmount.negate());
            txns.setWinAmount(betAmount);
            //???????????????????????????
            txns.setBetType(table.getString("id"));
            //???????????????????????????????????????????????????0??????????????????
//            txns.setWinningAmount(BigDecimal.ZERO);
            //??????????????????
            txns.setBetTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //??????????????????,???????????????????????????
            txns.setRealBetAmount(betAmount);
            //??????????????????,????????????
            txns.setRealWinAmount(BigDecimal.ZERO);
            //???????????? (??????????????????)
            //??????????????? : ???:0,???:1,??????:2
            //?????????????????? ??? ????????????
            txns.setTurnover(betAmount);
            //????????????????????????
            txns.setTxTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //????????????
            txns.setMethod("Place Bet");
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
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }

            jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
            jsonObject.put("bonus", 0);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            jsonObject.put("balance", BigDecimal.ZERO);
            jsonObject.put("bonus", 0);
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    @Override
    public Object credit(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_credit redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }
            BigDecimal balance = memBaseinfo.getBalance();
            JSONObject transaction = params.getJSONObject("transaction");
            String platformTxId = transaction.getString("id");
            String refId = transaction.getString("refId");
            JSONObject game = params.getJSONObject("game");
//            Txns oldTxns1 = getTxns(platformTxId, memBaseinfo.getAccount());
//            if(null!=oldTxns1){
//                jsonObject.put("balance", balance);
//                jsonObject.put("bonus", 0);
//                jsonObject.put("status", "BET_ALREADY_EXIST");
//                return jsonObject;
//            }
            // ????????????????????????
            Txns oldTxns = getTxnsByRoundId(refId, memBaseinfo.getAccount());
            if (null == oldTxns) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "BET_DOES_NOT_EXIST");
                return jsonObject;
            }
            // ????????????????????????
            if ("Cancel Bet".equals(oldTxns.getMethod())||"Settle".equals(oldTxns.getMethod())) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "BET_ALREADY_SETTLED");
                return jsonObject;
            }
            // ????????????
            BigDecimal betAmount = null!=transaction.getBigDecimal("amount")?transaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;
            // ??????????????????0
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INSUFFICIENT_FUNDS");
                return jsonObject;
            }
            if (betAmount.compareTo(BigDecimal.ZERO) != 0) {
                // ????????????
                balance =balance.add(betAmount);
                // ??????????????????
                gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.SETTLE, TradingEnum.INCOME);
            }

            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxns, txns);
            //??????????????????
            txns.setPlatformTxId(platformTxId);
            txns.setBalance(balance);
            txns.setId(null);
            //????????????
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            //???????????????????????????????????????????????????0??????????????????
            txns.setWinningAmount(betAmount);
            txns.setWinAmount(betAmount);
            //??????????????? : ???:0,???:1,??????:2
            int resultTyep;
            if (betAmount.compareTo(BigDecimal.ZERO) == 0) {
                resultTyep = 2;
            } else if (betAmount.compareTo(BigDecimal.ZERO) > 0) {
                resultTyep = 0;
            } else {
                resultTyep = 1;
            }
            oldTxns.setResultType(resultTyep);
            //????????????
            txns.setMethod("Settle");
            txns.setStatus("Running");
            txns.setCreateTime(dateStr);
            txnsMapper.insert(txns);

            oldTxns.setStatus("Settle");
            oldTxns.setUpdateTime(dateStr);
            txnsMapper.updateById(oldTxns);

            jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
            jsonObject.put("bonus", 0);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            jsonObject.put("balance", BigDecimal.ZERO);
            jsonObject.put("bonus", 0);
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    @Override
    public Object cancel(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_cancel redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);

        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }
            JSONObject transaction = params.getJSONObject("transaction");
            String platformTxId = transaction.getString("id");
            String refId = transaction.getString("refId");
//            JSONObject game = params.getJSONObject("game");
//            JSONObject table = game.getJSONObject("details").getJSONObject("table");


            // ????????????????????????
//            Txns oldTxns1 = getTxns(platformTxId, memBaseinfo.getAccount());
            BigDecimal balance = memBaseinfo.getBalance();
//            if(null!=oldTxns1){
//                jsonObject.put("balance", balance);
//                jsonObject.put("bonus", 0);
//                jsonObject.put("status", "BET_ALREADY_EXIST");
//                return jsonObject;
//            }
            // ????????????????????????
            Txns oldTxns = getTxnsByRoundId(refId, memBaseinfo.getAccount());
            if (null == oldTxns) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "BET_DOES_NOT_EXIST");
                return jsonObject;
            }

            // ????????????????????????
            if ("Cancel Bet".equals(oldTxns.getMethod())||"Settle".equals(oldTxns.getMethod())) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "BET_ALREADY_SETTLED");
                return jsonObject;
            }
            BigDecimal betAmount = null!=transaction.getBigDecimal("amount")?transaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;
            BigDecimal amount = oldTxns.getBetAmount();

            // ??????????????????????????????
            if (betAmount.compareTo(amount) == 1) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INSUFFICIENT_FUNDS");
                return jsonObject;
            }else {
                balance = balance.add(betAmount);
                gameCommonService.updateUserBalance(memBaseinfo, amount, GoldchangeEnum.REFUND, TradingEnum.INCOME);
            }

            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);

            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxns, txns);
            //??????????????????
            txns.setPlatformTxId(platformTxId);
            txns.setBalance(balance);
            txns.setId(null);
            txns.setStatus("Running");
            txns.setWinAmount(amount);
            txns.setRealWinAmount(amount);//??????????????????
            txns.setMethod("Cancel Bet");
            txns.setCreateTime(dateStr);
            if (betAmount.compareTo(amount) == 1) {
                Txns txns2 = new Txns();
                BeanUtils.copyProperties(oldTxns, txns2);
                //??????????????????
                txns2.setPlatformTxId(platformTxId);
                txns2.setBalance(balance);
                txns2.setId(null);
                txns2.setStatus("Running");
                txns2.setWinningAmount(amount.subtract(betAmount).negate());
                txns2.setWinAmount(amount);
                txns2.setRealWinAmount(amount);//??????????????????
                txns2.setMethod("Place Bet");
                txns2.setCreateTime(dateStr);
                txnsMapper.insert(txns2);
                txns.setStatus("Place Bet");
            }
            txnsMapper.insert(txns);

            oldTxns.setStatus("Cancel Bet");
            oldTxns.setUpdateTime(dateStr);
            txnsMapper.updateById(oldTxns);
            // ??????????????????0


            jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
            jsonObject.put("bonus", 0);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            jsonObject.put("balance", BigDecimal.ZERO);
            jsonObject.put("bonus", 0);
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    @Override
    public Object promo_payout(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_promo_payout  redtigerGame paramJson:{}, ip:{}", params, ip);
        JSONObject jsonObject = new JSONObject();
        String uuid = params.getString("uuid");
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // ??????IP
            if (checkIp(ip, platformGameParent)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "TEMPORARY_ERROR");
                return jsonObject;
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
//                jsonObject.put("balance", BigDecimal.ZERO);
//                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_PARAMETER");
                return jsonObject;
            }
            JSONObject promoTransaction = params.getJSONObject("promoTransaction");
            String platformTxId = promoTransaction.getString("id");
            JSONObject game = params.getJSONObject("game");
            JSONObject table = null;
            if(null!=game){
                table = game.getJSONObject("details").getJSONObject("table");
            }

            // ????????????
            BigDecimal betAmount = BigDecimal.ZERO;
            String roundId = null, promotionId = null, promoType = promoTransaction.getString("type");
            GamePlatform gamePlatform;
            if("Y".equals(OpenAPIProperties.REDTIGER_IS_PLATFORM_LOGIN)){
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(platformGameParent.getPlatformCode(),platformGameParent.getPlatformCode());
            }else if(null!=table){
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(table.getString("id"), platformGameParent.getPlatformCode());

            }else {
                gamePlatform = gameCommonService.getGamePlatformByParentName(OpenAPIProperties.REDTIGER_PLATFORM_CODE).get(0);
            }
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
            BigDecimal balance = memBaseinfo.getBalance();
            // ??????????????????
            if ("FreeRoundPlayableSpent".equals(promoType)) {
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;
                roundId = promoTransaction.getString("voucherId");
            } else if ("JackpotWin".equals(promoType)) {
                // ??????????????????
                roundId = game.getJSONObject("details").getJSONObject("table").getString("id");
                promotionId = roundId;
                JSONArray jsonArray = promoTransaction.getJSONArray("jackpots");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    betAmount.add(null!=jsonObject1.getBigDecimal("winAmount")?jsonObject1.getBigDecimal("winAmount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO);
                }
            } else if ("RewardGamePlayableSpent".equals(promoType)) {
                // ??????????????????????????????????????????????????????????????????????????????
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;
                roundId = promoTransaction.getString("voucherId");
            } else if ("RewardGameWinCapReached".equals(promoType)) {
                // ?????????????????????????????????????????????????????????????????????
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;;
                roundId = promoTransaction.getString("voucherId");
            } else if ("RewardGameMinBetLimitReached".equals(promoType)) {

                // ??????????????????????????????????????????????????????????????????????????????????????????????????????
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;;
                roundId = promoTransaction.getString("voucherId");
            } else if ("RtrMonetaryReward".equals(promoType)) {
                // ?????????????????????????????????????????????????????????????????????
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;;
                roundId = promoTransaction.getString("bonusConfigId");
            } else if ("roulette".equals(promoType)) {

                // ???????????????????????????????????????????????????
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;;
                roundId = game.getJSONObject("details").getJSONObject("table").getString("id");
                promotionId = roundId;
            }else {
                betAmount = null!=promoTransaction.getBigDecimal("amount")?promoTransaction.getBigDecimal("amount").multiply(platformGameParent.getCurrencyPro()):BigDecimal.ZERO;;
                roundId = game.getJSONObject("details").getJSONObject("table").getString("id");
            }

            // ??????????????????0
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INSUFFICIENT_FUNDS");
                return jsonObject;
            }

            // ????????????????????????
            Txns txns = getTxns(platformTxId, memBaseinfo.getAccount());
            if (null != txns) {
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
//                jsonObject.put("status", "BET_DOES_NOT_EXIST");
                return jsonObject;
            }
            txns = new Txns();
            // ????????????
            balance = balance.add(betAmount);
            // ??????????????????
            gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.ACTIVITY_GIVE, TradingEnum.INCOME);

            //??????????????????
            txns.setPlatformTxId(platformTxId);
            //???????????????????????? true????????? false ???
            txns.setBet(false);
            //?????? ID
            txns.setUserId(memBaseinfo.getAccount());
            //??????????????????
            txns.setCurrency(params.getString("currency"));
            //???????????????????????????
//            txns.setBetType(promoTransaction.getString("type"));
            // ????????????
//            txns.setHasBonusGame(1);
            //????????????
            txns.setPlatform(platformGameParent.getPlatformCode());
            //????????????
            txns.setPlatformEnName(platformGameParent.getPlatformEnName());
            txns.setPlatformCnName(platformGameParent.getPlatformCnName());
            txns.setRoundId(roundId);
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
            txns.setBetAmount(BigDecimal.ZERO);
            //???????????????????????????????????????????????????0??????????????????
            txns.setWinningAmount(betAmount);
            //??????????????????
            txns.setBetTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //??????????????????,???????????????????????????
            txns.setRealBetAmount(BigDecimal.ZERO);
            //??????????????????,????????????
            txns.setRealWinAmount(betAmount);
            //???????????? (??????????????????)
            txns.setWinAmount(betAmount);
            // ????????????
            txns.setAmount(betAmount);
            // ??????ID
            txns.setPromotionId(promotionId);
            // ????????????ID
            txns.setPromotionTypeId(promoType);
            //?????????????????? ??? ????????????
            txns.setTurnover(BigDecimal.ZERO);
            //????????????????????????
            txns.setTxTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //????????????
            txns.setMethod("Bonus");
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
                jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
                jsonObject.put("bonus", 0);
                jsonObject.put("status", "INVALID_TOKEN_ID");
                return jsonObject;
            }

            jsonObject.put("balance", balance.divide(platformGameParent.getCurrencyPro()));
            jsonObject.put("bonus", 0);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            jsonObject.put("balance", BigDecimal.ZERO);
            jsonObject.put("bonus", 0);
            jsonObject.put("status", "INVALID_TOKEN_ID");
            return jsonObject;
        }
    }

    /**
     * ??????????????????
     *
     * @param reference ????????????
     * @param userId    ??????ID
     * @return ??????
     */
    private Txns getTxns(String reference, String userId) {
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet")
                .or().eq(Txns::getMethod, "Cancel Bet")
                .or().eq(Txns::getMethod, "Bonus")
                .or().eq(Txns::getMethod, "Settle"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, reference);
        wrapper.eq(Txns::getPlatform, OpenAPIProperties.REDTIGER_PLATFORM_CODE);
//        wrapper.eq(Txns::getUserId, userId);
        return txnsMapper.selectOne(wrapper);
    }
    private Txns getTxnsByRoundId(String roundId, String userId) {
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet")
                .or().eq(Txns::getMethod, "Cancel Bet")
                .or().eq(Txns::getMethod, "Bonus")
                .or().eq(Txns::getMethod, "Settle"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getRoundId, roundId);
        wrapper.eq(Txns::getPlatform, OpenAPIProperties.REDTIGER_PLATFORM_CODE);
//        wrapper.eq(Txns::getUserId, userId);
        return txnsMapper.selectOne(wrapper);
    }


    /**
     * ??????IP????????????
     *
     * @param ip ip
     * @return boolean
     */
    private boolean checkIp(String ip, GameParentPlatform platformGameParent) {
        if (null == platformGameParent) {
            return true;
        } else if (null == platformGameParent.getIpAddr() || "".equals(platformGameParent.getIpAddr())) {
            return false;
        }
        return !platformGameParent.getIpAddr().equals(ip);

    }

    private GameParentPlatform getGameParentPlatform() {
        return gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.REDTIGER_PLATFORM_CODE);
    }
}
