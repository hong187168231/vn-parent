package com.indo.game.service.dj.impl;

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
import com.indo.game.pojo.dto.dj.DjCallBackParentReq;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.dj.DjCallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;


/**
 * PG
 *
 * @author
 */
@Slf4j
@Service
public class DjCallbackServiceImpl implements DjCallbackService {

    @Resource
    private GameCommonService gameCommonService;

    @Autowired
    private CptOpenMemberService externalService;

    @Autowired
    private TxnsMapper txnsMapper;

    @Override
    public Object getBalance(DjCallBackParentReq djCallBackParentReq, String ip) {
//        CptOpenMember cptOpenMember = externalService.getCptOpenMember(Integer.parseInt(djCallBackParentReq.getLogin_id()), OpenAPIProperties.DJ_PLATFORM_CODE);
        StringBuilder stringBuilder = new StringBuilder();
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(djCallBackParentReq.getLogin_id());
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.DJ_PLATFORM_CODE);
        stringBuilder.append("<?xml version=\"1.0\" ?>").append("<get_balance>").append("<status_code>");
        if (memBaseinfo == null) {
            stringBuilder.append("99</status_code>").append("<status_text>OK</status_text>").append("<balance>");
            stringBuilder.append("0").append("</balance></get_balance>");
            return stringBuilder;
        }

        stringBuilder.append("00</status_code>").append("<status_text>OK</status_text>").append("<balance>");
        stringBuilder.append(memBaseinfo.getBalance().divide(gameParentPlatform.getCurrencyPro())).append("</balance></get_balance>");
        return stringBuilder;
    }

    @Override
    public Object djBetCallback(DjCallBackParentReq djCallBackParentReq, String ip) {
//        CptOpenMember cptOpenMember = externalService.getCptOpenMember(Integer.parseInt(djCallBackParentReq.getLogin_id()), OpenAPIProperties.DJ_PLATFORM_CODE);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" ?>").append("<bet>").append("<status_code>");
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.DJ_PLATFORM_CODE);
        GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(OpenAPIProperties.DJ_PLATFORM_CODE,gameParentPlatform.getPlatformCode());
        GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(djCallBackParentReq.getLogin_id());
        BigDecimal balance = memBaseinfo.getBalance();
        BigDecimal betAmount = null!=djCallBackParentReq.getStake_money()?djCallBackParentReq.getStake_money().multiply(gameParentPlatform.getCurrencyPro()):BigDecimal.ZERO;
        if (betAmount.compareTo(BigDecimal.ZERO) == -1 && memBaseinfo.getBalance().compareTo(betAmount) == -1) {
            stringBuilder.append("88</status_code>").append("<status_text>Insufficient fund to bet </status_text>");
            stringBuilder.append("<ref_id>").append(djCallBackParentReq.getTicket_id()).append("</ref_id>").append("<balance>");
            stringBuilder.append(balance.divide(gameParentPlatform.getCurrencyPro())).append("</balance></bet>");
            return stringBuilder;
        }
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Settle"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, djCallBackParentReq.getTicket_id());
        wrapper.eq(Txns::getPlatform, OpenAPIProperties.DJ_PLATFORM_CODE);

        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null != oldTxns) {
            if ("Cancel Bet".equals(oldTxns.getMethod())||"Settle".equals(oldTxns.getMethod())) {
                stringBuilder.append("All Other Code</status_code>").append("<status_text>General error </status_text>");
                stringBuilder.append("<ref_id>").append(djCallBackParentReq.getTicket_id()).append("</ref_id>").append("<balance>");
                stringBuilder.append(balance.divide(gameParentPlatform.getCurrencyPro())).append("</balance></bet>");
                return stringBuilder;
            }
        }
        if (null != oldTxns) {
            if (betAmount.compareTo(BigDecimal.ZERO) != 0) {
                if (betAmount.compareTo(BigDecimal.ZERO) == -1) {
                    balance = balance.subtract(betAmount.abs());
                    gameCommonService.updateUserBalance(memBaseinfo, betAmount.abs(), GoldchangeEnum.SETTLE, TradingEnum.SPENDING);
                } else {
                    balance = balance.add(betAmount);
                    gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.SETTLE, TradingEnum.INCOME);
                }
            }
        }else {
            if (betAmount.compareTo(BigDecimal.ZERO) != 0) {
                if (betAmount.compareTo(BigDecimal.ZERO) == -1) {
                    balance = balance.subtract(betAmount.abs());
                    gameCommonService.updateUserBalance(memBaseinfo, betAmount.abs(), GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);
                } else {
                    balance = balance.subtract(betAmount);
                    gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);
                }
            }
        }

        Txns txns = new Txns();
        //??????????????????
        txns.setPlatformTxId(djCallBackParentReq.getTicket_id());
        //?????????
        txns.setRoundId(djCallBackParentReq.getTicket_id());
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
        if (null != oldTxns) {
            txns.setWinningAmount(betAmount);
        }else {
            if (betAmount.compareTo(BigDecimal.ZERO) == -1) {
                txns.setWinningAmount(betAmount);
            }else {
                txns.setWinningAmount(betAmount.negate());
            }
        }
        txns.setWinAmount(betAmount);
        //??????????????????
        txns.setBetTime(DateUtils.formatByString(djCallBackParentReq.getCreated_datetime(), DateUtils.newFormat));
        //??????????????????,???????????????????????????
        txns.setRealBetAmount(betAmount);
        //???????????? (??????????????????)
        //?????????????????? ??? ????????????
        txns.setTurnover(betAmount);
        //????????????????????????
        txns.setTxTime(null != djCallBackParentReq.getCreated_datetime() ? DateUtils.formatByString(djCallBackParentReq.getCreated_datetime(), DateUtils.newFormat) : "");
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
            oldTxns.setStatus("Settle");
            txnsMapper.updateById(oldTxns);
            txns.setMethod("Settle");
        }

        int num = txnsMapper.insert(txns);
        if (num <= 0) {
            stringBuilder.append("All Other Code</status_code>").append("<status_text>General error </status_text>");
            stringBuilder.append("<ref_id>").append(djCallBackParentReq.getTicket_id()).append("</ref_id>").append("<balance>");
            stringBuilder.append(balance.divide(gameParentPlatform.getCurrencyPro())).append("</balance></bet>");
            return stringBuilder;
        }
        stringBuilder.append("00</status_code>").append("<status_text>OK</status_text>");
        stringBuilder.append("<ref_id>").append(djCallBackParentReq.getTicket_id()).append("</ref_id>").append("<balance>");
        stringBuilder.append(balance.divide(gameParentPlatform.getCurrencyPro())).append("</balance></bet>");

        return stringBuilder;
    }

    @Override
    public Object djRefundtCallback(DjCallBackParentReq djCallBackParentReq, String ip) {
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Settle"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, djCallBackParentReq.getTicket_id());
        wrapper.eq(Txns::getPlatform, OpenAPIProperties.DJ_PLATFORM_CODE);
        
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null == oldTxns) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<?xml version=\"1.0\" ?>").append("<cancel_bet>").append("<status_code>");
            stringBuilder.append("All Other Code</status_code>").append("<status_text>General error </status_text>");
            stringBuilder.append("</cancel_bet>");
            return stringBuilder;
        }
        if (null != oldTxns) {
            if ("Cancel Bet".equals(oldTxns.getMethod())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<?xml version=\"1.0\" ?>").append("<cancel_bet>").append("<status_code>");
                stringBuilder.append("All Other Code</status_code>").append("<status_text>General error </status_text>");
                stringBuilder.append("</cancel_bet>");
                return stringBuilder;
            }
        }
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(oldTxns.getUserId());
        BigDecimal balance = memBaseinfo.getBalance();
        BigDecimal amount = oldTxns.getWinningAmount();
        if ("Place Bet".equals(oldTxns.getMethod())) {
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                if (amount.compareTo(BigDecimal.ZERO) == -1) {//??????0
                    balance = balance.add(amount.abs());
                    gameCommonService.updateUserBalance(memBaseinfo, amount.abs(), GoldchangeEnum.CANCEL_BET, TradingEnum.INCOME);
                } else {//??????0
                    balance = balance.add(amount);
                    gameCommonService.updateUserBalance(memBaseinfo, amount, GoldchangeEnum.CANCEL_BET, TradingEnum.SPENDING);
                }
            }
        }else {
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                if (amount.compareTo(BigDecimal.ZERO) == -1) {//??????0
                    balance = balance.add(amount.abs());
                    gameCommonService.updateUserBalance(memBaseinfo, amount.abs(), GoldchangeEnum.UNSETTLE, TradingEnum.INCOME);
                } else {//??????0
                    balance = balance.add(amount);
                    gameCommonService.updateUserBalance(memBaseinfo, amount, GoldchangeEnum.UNSETTLE, TradingEnum.SPENDING);
                }
            }
        }
        String dateStr = DateUtils.format(new Date(), DateUtils.ISO8601_DATE_FORMAT);

        Txns txns = new Txns();
        BeanUtils.copyProperties(oldTxns, txns);
        //??????????????????
        txns.setPlatformTxId(djCallBackParentReq.getTicket_id());
        //?????????
        txns.setBalance(balance);
        txns.setId(null);
        txns.setMethod("Cancel Bet");
        txns.setStatus("Running");
        txns.setCreateTime(dateStr);
        txnsMapper.insert(txns);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" ?>").append("<cancel_bet>").append("<status_code>");
        stringBuilder.append("00</status_code>").append("<status_text>OK</status_text>");
        stringBuilder.append("</cancel_bet>");
        return stringBuilder;
    }

}
