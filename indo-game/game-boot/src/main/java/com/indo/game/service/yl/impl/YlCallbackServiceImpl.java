package com.indo.game.service.yl.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.utils.DateUtils;
import com.indo.core.mapper.game.TxnsMapper;
import com.indo.core.pojo.bo.MemTradingBO;
import com.indo.core.pojo.entity.game.GameCategory;
import com.indo.core.pojo.entity.game.GameParentPlatform;
import com.indo.core.pojo.entity.game.GamePlatform;
import com.indo.core.pojo.entity.game.Txns;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.yl.YlCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;


/**
 * PS
 *
 * @author
 */
@Service
public class YlCallbackServiceImpl implements YlCallbackService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private GameCommonService gameCommonService;
    @Autowired
    private TxnsMapper txnsMapper;


    @Override
    public Object ylGetBalanceCallback(JSONObject jsonObject) {
        JSONObject dataJson = new JSONObject();
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("userId"));
        if (null == memBaseinfo) {
            dataJson.put("status", 500);
            dataJson.put("msg", "system busy");
            return dataJson;
        }
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.YL_PLATFORM_CODE);
        dataJson.put("status", 200);
        dataJson.put("balance", memBaseinfo.getBalance().divide(gameParentPlatform.getCurrencyPro()));
        return dataJson;
    }

    @Override
    public Object psBetCallback(JSONObject jsonObject) {
        JSONObject dataJson = new JSONObject();
        try {
            String userId = jsonObject.getString("userId");
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("userId"));
            GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.YL_PLATFORM_CODE);
            GamePlatform gamePlatform;
            if("Y".equals(OpenAPIProperties.YL_IS_PLATFORM_LOGIN)){
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(gameParentPlatform.getPlatformCode(),gameParentPlatform.getPlatformCode());
            }else {
                gamePlatform = gameCommonService.getGamePlatformByplatformCode(jsonObject.getString("gameId"));
            }
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
            BigDecimal betMoney = null!=jsonObject.getBigDecimal("requireAmount")?jsonObject.getBigDecimal("requireAmount").multiply(gameParentPlatform.getCurrencyPro()):BigDecimal.ZERO;
            BigDecimal balance = memBaseinfo.getBalance();
            BigDecimal betAmount = null!=jsonObject.getBigDecimal("validbet")?jsonObject.getBigDecimal("validbet").multiply(gameParentPlatform.getCurrencyPro()):BigDecimal.ZERO;
            if (memBaseinfo.getBalance().compareTo(betMoney) == -1) {
                dataJson.put("status", 500);
                dataJson.put("msg", "code:9003");
                return dataJson;
            }
            String txId = jsonObject.getString("txId");
            LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
            wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
            wrapper.eq(Txns::getStatus, "Running");
            wrapper.eq(Txns::getPlatformTxId, txId);
            wrapper.eq(Txns::getUserId, memBaseinfo.getId());
            Txns oldTxns = txnsMapper.selectOne(wrapper);
            if (null != oldTxns) {
                if ("Cancel Bet".equals(oldTxns.getMethod())) {
                    dataJson.put("status", 500);
                    dataJson.put("msg", "code:9002");
                    return dataJson;
                } else {
                    dataJson.put("status", 500);
                    dataJson.put("msg", "code:9002");
                    return dataJson;
                }
            }
            BigDecimal winAmount = jsonObject.getBigDecimal("profit");
            if (winAmount.compareTo(BigDecimal.ZERO) == 1) {//???
                balance = balance.add(winAmount).add(betAmount);
                gameCommonService.updateUserBalance(memBaseinfo, winAmount.add(betAmount), GoldchangeEnum.PLACE_BET, TradingEnum.INCOME);
            }
            if (memBaseinfo.getBalance().compareTo(betAmount.abs()) == -1) {
                dataJson.put("status", 500);
                dataJson.put("msg", "code:9003");
                return dataJson;
            }
            balance = balance.subtract(betAmount.abs());
            gameCommonService.updateUserBalance(memBaseinfo, betAmount.abs(), GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);

            Txns txns = new Txns();
            if(null!=oldTxns) {
                BeanUtils.copyProperties(oldTxns, txns);
            }else {
                //??????????????????
                txns.setPlatformTxId(txId);
                //???????????????????????? true????????? false ???
                //?????? ID
                txns.setUserId(userId);
                //??????????????????
                txns.setCurrency(gameParentPlatform.getCurrencyType());
                //????????????
                txns.setPlatform(gameParentPlatform.getPlatformCode());
                //????????????
                txns.setPlatformEnName(gameParentPlatform.getPlatformEnName());
                txns.setPlatformCnName(gameParentPlatform.getPlatformCnName());
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
                txns.setWinningAmount(winAmount);
                //??????????????????transTime
                txns.setBetTime(DateUtils.format(jsonObject.getDate("transTime"), DateUtils.newFormat));
                //??????????????????,???????????????????????????
                txns.setRealBetAmount(betAmount);
                //??????????????????,????????????
                txns.setRealWinAmount(winAmount);
                //???????????? (??????????????????)
                //??????????????? : ???:0,???:1,??????:2
                int resultTyep;
                if (winAmount.compareTo(BigDecimal.ZERO) == 1) {
                    resultTyep = 0;
                } else {
                    resultTyep = 1;
                }
                txns.setResultType(resultTyep);
                //?????????????????? ??? ????????????
                txns.setTurnover(betAmount);
                //????????????????????????
                txns.setTxTime(null != jsonObject.getDate("transTime") ? DateUtils.format(jsonObject.getDate("transTime"), DateUtils.newFormat) : "");
                //????????????
                txns.setMethod("Place Bet");
                txns.setStatus("Running");
                //??????
                txns.setBalance(balance);
                //????????????
                String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
                txns.setCreateTime(dateStr);
            }
            if (oldTxns != null) {
                //????????????
                txns.setMethod("Settle");
                oldTxns.setStatus("Settle");
                txnsMapper.updateById(oldTxns);
            }
            int num = txnsMapper.insert(txns);
            if (num <= 0) {
                dataJson.put("status", 500);
                dataJson.put("msg", "code:9003");
                return dataJson;
            }
            dataJson.put("status", 200);
            dataJson.put("balance", balance.divide(gameParentPlatform.getCurrencyPro()));
            return dataJson;
        } catch (Exception e) {
            logger.error("YL???????????????????????????", e);
            e.printStackTrace();
        }
        dataJson.put("status", 500);
        dataJson.put("msg", "code:9003");
        return dataJson;
    }

    @Override
    public Object ylVoidFishBetCallback(JSONObject jsonObject) {
        JSONObject dataJson = new JSONObject();
        try {
            GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.YL_PLATFORM_CODE);
            String txId = jsonObject.getString("txId");
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("userId"));
            LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Txns::getPromotionTxId, txId);
            wrapper.eq(Txns::getPlatform, jsonObject.getString("gameId"));
            wrapper.eq(Txns::getUserId, memBaseinfo.getId());
            Txns oldTxns = txnsMapper.selectOne(wrapper);
            if (null == oldTxns) {
                dataJson.put("status_code", "500");
                dataJson.put("message", "1029 settled transaction id");
                return dataJson;
            }
            BigDecimal balance = memBaseinfo.getBalance().add(oldTxns.getAmount());
            gameCommonService.updateUserBalance(memBaseinfo, oldTxns.getAmount(), GoldchangeEnum.REFUND, TradingEnum.INCOME);
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);

            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxns, txns);
            //??????????????????
            txns.setPlatformTxId(txId);
            //?????????
            txns.setBalance(balance);
            txns.setId(null);
            txns.setMethod("Cancel Bet");
            txns.setStatus("Running");
            txns.setCreateTime(dateStr);

            txnsMapper.insert(txns);
            dataJson.put("status", 200);
            dataJson.put("balance", balance.divide(gameParentPlatform.getCurrencyPro()));
            return dataJson;
        } catch (Exception e) {
            logger.error("YL?????????????????????????????????????????????", e);
            e.printStackTrace();
        }
        dataJson.put("status_code", "500");
        dataJson.put("message", "1029 settled transaction id");
        return dataJson;
    }

}

