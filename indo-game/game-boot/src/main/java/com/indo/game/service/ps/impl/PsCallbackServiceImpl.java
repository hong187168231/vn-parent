package com.indo.game.service.ps.impl;

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
import com.indo.game.pojo.dto.ps.PsCallBackParentReq;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.ps.PsCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;


/**
 * PS
 *
 * @author
 */
@Service
public class PsCallbackServiceImpl implements PsCallbackService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private GameCommonService gameCommonService;
    @Autowired
    private CptOpenMemberService externalService;
    @Autowired
    private TxnsMapper txnsMapper;

    private static final DecimalFormat format = new DecimalFormat("#");

    @Override
    public Object psVerifyCallback(PsCallBackParentReq psCallBackParentReq, String ip) {
        CptOpenMember cptOpenMember = externalService.quertCptOpenMember(psCallBackParentReq.getAccess_token(), OpenAPIProperties.PS_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("code", 1);
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        if (cptOpenMember == null) {
            dataJson.put("code", 1);
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PS_PLATFORM_CODE);
        String signPrice = format.format(memBaseinfo.getBalance().divide(gameParentPlatform.getCurrencyPro()).multiply(new BigDecimal(100)));
        dataJson.put("status_code", 0);
        dataJson.put("member_id", cptOpenMember.getId() + "");
        dataJson.put("member_name", cptOpenMember.getUserName());
        dataJson.put("balance", Integer.parseInt(signPrice));
        return dataJson;
    }

    @Override
    public Object psBetCallback(PsCallBackParentReq psbetCallBackReq, String ip) {
        CptOpenMember cptOpenMember = externalService.quertCptOpenMember(psbetCallBackReq.getAccess_token(), OpenAPIProperties.PS_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("status_code", "1");
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PS_PLATFORM_CODE);
        GamePlatform gamePlatform;
        if("Y".equals(OpenAPIProperties.PS_IS_PLATFORM_LOGIN)){
            gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(gameParentPlatform.getPlatformCode(),gameParentPlatform.getPlatformCode());
        }else {
            gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(String.valueOf(psbetCallBackReq.getGame_id()), gameParentPlatform.getPlatformCode());

        }
        GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
        BigDecimal balance = memBaseinfo.getBalance();
        BigDecimal betAmount = new BigDecimal(psbetCallBackReq.getTotal_bet()).divide(new BigDecimal(100)).multiply(gameParentPlatform.getCurrencyPro());
//        BigDecimal betAmount = new BigDecimal(psbetCallBackReq.getTotal_bet()).multiply(gameParentPlatform.getCurrencyPro());
        if (memBaseinfo.getBalance().compareTo(betAmount) == -1) {
            dataJson.put("status_code", "3");
            dataJson.put("message", "????????????");
            return dataJson;
        }
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, psbetCallBackReq.getTxn_id());
        wrapper.eq(Txns::getUserId, memBaseinfo.getAccount());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null != oldTxns) {
            if ("Cancel Bet".equals(oldTxns.getMethod())) {
                dataJson.put("status_code", "2");
                dataJson.put("message", "????????????");
                return dataJson;
            } else {
                dataJson.put("status_code", "2");
                dataJson.put("message", "????????????");
                return dataJson;
            }
        }

        balance = balance.subtract(betAmount);
        gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);

        Txns txns = new Txns();
        //??????????????????
        txns.setPlatformTxId(psbetCallBackReq.getTxn_id());
        //?????????
        txns.setRoundId(psbetCallBackReq.getTxn_id());
        //???????????????????????? true????????? false ???
        //?????? ID
        txns.setUserId(memBaseinfo.getAccount());
        //??????????????????
        txns.setCurrency(gameParentPlatform.getCurrencyType());
        //????????????
        txns.setPlatform(gameParentPlatform.getPlatformCode());
        //??????????????????
        txns.setPlatformEnName(gameParentPlatform.getPlatformEnName());
        //??????????????????
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
        txns.setWinningAmount(betAmount.negate());
        txns.setWinAmount(betAmount);
        //??????????????????
        txns.setBetTime(DateUtils.formatByString(psbetCallBackReq.getTs(), DateUtils.newFormat));
        //??????????????????,???????????????????????????
        txns.setRealBetAmount(betAmount);
        //???????????? (??????????????????)
        //?????????????????? ??? ????????????
        txns.setTurnover(betAmount);
        //????????????????????????
        txns.setTxTime(null != psbetCallBackReq.getTs() ? DateUtils.formatByString(psbetCallBackReq.getTs(), DateUtils.newFormat) : "");
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
        if (oldTxns != null) {
            txns.setStatus("Settle");
            txnsMapper.updateById(oldTxns);
        }
        int num = txnsMapper.insert(txns);
        if (num <= 0) {
            dataJson.put("status_code", "2");
            dataJson.put("message", "????????????");
            return dataJson;
        }
        String signPrice = format.format(balance.divide(gameParentPlatform.getCurrencyPro()).multiply(new BigDecimal(100)));
        dataJson.put("status_code", 0);
        dataJson.put("balance", Integer.parseInt(signPrice));
        return dataJson;
    }

    @Override
    public Object psResultCallback(PsCallBackParentReq psbetCallBackReq, String ip) {
        CptOpenMember cptOpenMember = externalService.quertCptOpenMember(psbetCallBackReq.getAccess_token(), OpenAPIProperties.PS_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("status_code", "1");
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PS_PLATFORM_CODE);
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Txns::getPlatformTxId, psbetCallBackReq.getTxn_id());
        wrapper.eq(Txns::getUserId, memBaseinfo.getAccount());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null == oldTxns) {
            dataJson.put("status_code", "2");
            dataJson.put("message", "????????????");
            return dataJson;
        }
//        BigDecimal money = new BigDecimal(psbetCallBackReq.getTotal_win()).subtract(new BigDecimal(psbetCallBackReq.getBonus_win()));
        BigDecimal money = new BigDecimal(psbetCallBackReq.getTotal_win()).divide(new BigDecimal(100)).multiply(gameParentPlatform.getCurrencyPro());
//        BigDecimal winAmount = money.divide(new BigDecimal(100));
        BigDecimal winAmount = money;
        BigDecimal balance = memBaseinfo.getBalance();
        balance = balance.add(winAmount);
        gameCommonService.updateUserBalance(memBaseinfo, winAmount, GoldchangeEnum.PLACE_BET, TradingEnum.INCOME);

        Txns txns = new Txns();
        BeanUtils.copyProperties(oldTxns, txns);
        txns.setId(null);
        txns.setBetAmount(oldTxns.getBetAmount());
        //??????????????????
        txns.setPlatformTxId(psbetCallBackReq.getTxn_id());
        int resultTyep = 0;
        txns.setResultType(resultTyep);
        txns.setWinningAmount(winAmount);
        txns.setWinAmount(winAmount);
        txns.setBalance(balance);
        txns.setStatus("Running");
        txns.setMethod("Settle");
        String dateStr = DateUtils.format(new Date(), DateUtils.ISO8601_DATE_FORMAT);
        txns.setCreateTime(dateStr);
        txnsMapper.insert(txns);
        oldTxns.setStatus("Settle");
        oldTxns.setUpdateTime(dateStr);
        txnsMapper.updateById(oldTxns);
        String signPrice = format.format(balance.divide(gameParentPlatform.getCurrencyPro()).multiply(new BigDecimal(100)));
        dataJson.put("status_code", 0);
        dataJson.put("balance", Integer.parseInt(signPrice));
        return dataJson;
    }

    @Override
    public Object psRefundtCallback(PsCallBackParentReq psbetCallBackReq, String ip) {

        CptOpenMember cptOpenMember = externalService.quertCptOpenMember(psbetCallBackReq.getAccess_token(), OpenAPIProperties.PS_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("status_code", "1");
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PS_PLATFORM_CODE);
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, psbetCallBackReq.getTxn_id());
        wrapper.eq(Txns::getUserId, memBaseinfo.getAccount());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null == oldTxns) {
            dataJson.put("status_code", "2");
            dataJson.put("message", "????????????");
            return dataJson;
        }
        BigDecimal balance = memBaseinfo.getBalance().add(oldTxns.getAmount());
        gameCommonService.updateUserBalance(memBaseinfo, oldTxns.getAmount(), GoldchangeEnum.REFUND, TradingEnum.INCOME);
        String dateStr = DateUtils.format(new Date(), DateUtils.ISO8601_DATE_FORMAT);

        Txns txns = new Txns();
        BeanUtils.copyProperties(oldTxns, txns);
        //??????????????????
        txns.setPlatformTxId(psbetCallBackReq.getTxn_id());
        txns.setWinningAmount(oldTxns.getAmount());
        //?????????
        txns.setBalance(balance);
        txns.setId(null);
        txns.setMethod("Refund");
        txns.setStatus("Running");
        txns.setCreateTime(dateStr);
        txnsMapper.insert(txns);
        String signPrice = format.format(balance.divide(gameParentPlatform.getCurrencyPro()).multiply(new BigDecimal(100)));
        dataJson.put("status_code", 0);
        dataJson.put("balance", Integer.parseInt(signPrice));
        return dataJson;
    }

    @Override
    public Object psBonusCallback(PsCallBackParentReq psbetCallBackReq, String ip) {
        CptOpenMember cptOpenMember = externalService.quertCptOpenMember(psbetCallBackReq.getAccess_token(), OpenAPIProperties.PS_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("status_code", "1");
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PS_PLATFORM_CODE);
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Txns::getMethod, "Give");
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, psbetCallBackReq.getTxn_id());
        wrapper.eq(Txns::getUserId, memBaseinfo.getAccount());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null == oldTxns) {
            dataJson.put("status_code", "2");
            dataJson.put("message", "????????????");
            return dataJson;
        }
        BigDecimal balance = memBaseinfo.getBalance();
//        BigDecimal amount = new BigDecimal(psbetCallBackReq.getBonus_reward()).divide(new BigDecimal(100));
        BigDecimal amount = new BigDecimal(psbetCallBackReq.getBonus_reward()).divide(new BigDecimal(100)).multiply(gameParentPlatform.getCurrencyPro());
        balance = balance.add(amount);
        gameCommonService.updateUserBalance(memBaseinfo, amount, GoldchangeEnum.ACTIVITY_GIVE, TradingEnum.INCOME);
        String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
        Txns txns = new Txns();
        BeanUtils.copyProperties(oldTxns, txns);
        //??????????????????
        txns.setPlatformTxId(psbetCallBackReq.getTxn_id());
        //?????????
        txns.setRoundId(psbetCallBackReq.getTxn_id());
        txns.setWinningAmount(amount);
        txns.setBalance(balance);
        txns.setMethod("Bonus");
        txns.setStatus("Running");
        txns.setCreateTime(dateStr);
        int number = txnsMapper.insert(txns);
        if (number <= 0) {
            dataJson.put("status_code", "2");
            dataJson.put("message", "????????????");
            return dataJson;
        }
        String signPrice = format.format(balance.divide(gameParentPlatform.getCurrencyPro()).multiply(new BigDecimal(100)));
        dataJson.put("status_code", 0);
        dataJson.put("balance", Integer.parseInt(signPrice));
        return dataJson;
    }

    @Override
    public Object psGetBalanceCallback(PsCallBackParentReq psbetCallBackReq, String ip) {
        CptOpenMember cptOpenMember = externalService.quertCptOpenMember(psbetCallBackReq.getAccess_token(), OpenAPIProperties.PS_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("status_code", "1");
            dataJson.put("message", "Token ??????");
            return dataJson;
        }
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.PS_PLATFORM_CODE);
        String signPrice = format.format(memBaseinfo.getBalance().divide(gameParentPlatform.getCurrencyPro()).multiply(new BigDecimal(100)));
        dataJson.put("status_code", 0);
        dataJson.put("balance", Integer.parseInt(signPrice));
        return dataJson;
    }
}

