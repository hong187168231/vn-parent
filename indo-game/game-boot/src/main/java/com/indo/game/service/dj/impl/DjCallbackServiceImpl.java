package com.indo.game.service.dj.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.result.Result;
import com.indo.common.utils.DateUtils;
import com.indo.common.web.util.http.HttpUtils;
import com.indo.game.common.util.SnowflakeId;
import com.indo.game.mapper.TxnsMapper;
import com.indo.game.pojo.dto.comm.ApiResponseData;
import com.indo.game.pojo.dto.dj.DjCallBackParentReq;
import com.indo.game.pojo.dto.ps.PsCallBackParentReq;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.game.pojo.entity.manage.GameCategory;
import com.indo.game.pojo.entity.manage.GameParentPlatform;
import com.indo.game.pojo.entity.manage.GamePlatform;
import com.indo.game.pojo.entity.manage.Txns;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.dj.DjCallbackService;
import com.indo.game.service.dj.DjService;
import com.indo.user.pojo.bo.MemTradingBO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;


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
        CptOpenMember cptOpenMember = externalService.getCptOpenMember(Integer.parseInt(djCallBackParentReq.getLogin_id()), OpenAPIProperties.DJ_PLATFORM_CODE);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" ?>").append("<get_balance>").append("<status_code>");
        if (cptOpenMember == null) {
            stringBuilder.append(">99</status_code>").append("<status_text>OK</status_text>").append("<balance>");
            stringBuilder.append("0").append("</balance></get_balance>");
            return stringBuilder;
        }
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        stringBuilder.append(">00</status_code>").append("<status_text>OK</status_text>").append("<balance>");
        stringBuilder.append(memBaseinfo.getBalance()).append("</balance></get_balance>");
        return stringBuilder;
    }

    @Override
    public Object djBetCallback(DjCallBackParentReq djCallBackParentReq, String ip) {
        CptOpenMember cptOpenMember = externalService.getCptOpenMember(Integer.parseInt(djCallBackParentReq.getLogin_id()), OpenAPIProperties.DJ_PLATFORM_CODE);
        JSONObject dataJson = new JSONObject();
        if (cptOpenMember == null) {
            dataJson.put("status_code", "1");
            dataJson.put("message", "Token 无效");
            return dataJson;
        }
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.DJ_PLATFORM_CODE);
        GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(OpenAPIProperties.DJ_PLATFORM_CODE,gameParentPlatform.getPlatformCode());
        GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        BigDecimal balance = memBaseinfo.getBalance();
        BigDecimal betAmount = djCallBackParentReq.getStake_money();
        if (memBaseinfo.getBalance().compareTo(betAmount) == -1) {
            dataJson.put("status_code", "3");
            dataJson.put("message", "馀额不足");
            return dataJson;
        }
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPromotionTxId, djCallBackParentReq.getTicket_id());

        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null != oldTxns) {
            if ("Cancel Bet".equals(oldTxns.getMethod())) {
                dataJson.put("status_code", "2");
                dataJson.put("message", "单号无效");
                return dataJson;
            } else {
                dataJson.put("status_code", "2");
                dataJson.put("message", "单号无效");
                return dataJson;
            }
        }

        balance = balance.subtract(betAmount);
        gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);

        Txns txns = new Txns();
        //游戏商注单号
        txns.setPlatformTxId(djCallBackParentReq.getTicket_id());
        //混合码
        txns.setRoundId(djCallBackParentReq.getTicket_id());
        //此交易是否是投注 true是投注 false 否
        //玩家 ID
        txns.setUserId(memBaseinfo.getAccount());
        //玩家货币代码
        txns.setCurrency(gameParentPlatform.getCurrencyType());
        //平台代码
        txns.setPlatform(gameParentPlatform.getPlatformCode());
        //平台英文名称
        txns.setPlatformEnName(gameParentPlatform.getPlatformEnName());
        //平台中文名称
        txns.setPlatformCnName(gameParentPlatform.getPlatformCnName());
        //平台游戏类型
        txns.setGameType(gameCategory.getGameType());
        //游戏分类ID
        txns.setCategoryId(gameCategory.getId());
        //游戏分类名称
        txns.setCategoryName(gameCategory.getGameName());
        //平台游戏代码
        txns.setGameCode(gamePlatform.getPlatformCode());
        //游戏名称
        txns.setGameName(gamePlatform.getPlatformEnName());
        //下注金额
        txns.setBetAmount(betAmount);
        //玩家下注时间
        txns.setBetTime(DateUtils.formatByString(djCallBackParentReq.getCreated_datetime(), DateUtils.newFormat));
        //真实下注金额,需增加在玩家的金额
        txns.setRealBetAmount(betAmount);
        //返还金额 (包含下注金额)
        //有效投注金额 或 投注面值
        txns.setTurnover(betAmount);
        //辨认交易时间依据
        txns.setTxTime(null != djCallBackParentReq.getCreated_datetime() ? DateUtils.formatByString(djCallBackParentReq.getCreated_datetime(), DateUtils.newFormat) : "");
        //操作名称
        txns.setMethod("Place Bet");
        txns.setStatus("Running");
        //余额
        txns.setBalance(balance);
        //创建时间
        String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
        txns.setCreateTime(dateStr);
        //投注 IP
        txns.setBetIp(ip);//  string 是 投注 IP
        if (oldTxns != null) {
            txns.setStatus("Settle");
            txnsMapper.updateById(oldTxns);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" ?>").append("<bet>").append("<status_code>");
        int num = txnsMapper.insert(txns);
        if (num <= 0) {
            dataJson.put("status_code", "2");
            dataJson.put("message", "单号无效");
            return dataJson;
        }
        stringBuilder.append(">00</status_code>").append("<status_text>OK</status_text>");
        stringBuilder.append("<ref_id>").append(memBaseinfo.getId()).append("</ref_id>").append("<balance>");
        stringBuilder.append(memBaseinfo.getBalance()).append("</balance></bet>");

        return dataJson;
    }

    @Override
    public Object djRefundtCallback(DjCallBackParentReq djCallBackParentReq, String ip) {
        CptOpenMember cptOpenMember = externalService.getCptOpenMember(Integer.parseInt(djCallBackParentReq.getLogin_id()), OpenAPIProperties.DJ_PLATFORM_CODE);
        if (cptOpenMember == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<?xml version=\"1.0\" ?>").append("<cancel_bet>>").append("<status_code>");
            stringBuilder.append(">99</status_code>").append("<status_text>fail</status_text>");
            stringBuilder.append("</cancel_bet>");
            return stringBuilder;
        }
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Settle"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPromotionTxId, djCallBackParentReq.getTicket_id());
        
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null == oldTxns) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<?xml version=\"1.0\" ?>").append("<cancel_bet>>").append("<status_code>");
            stringBuilder.append(">99</status_code>").append("<status_text>fail</status_text>");
            stringBuilder.append("</cancel_bet>");
            return stringBuilder;
        }
        BigDecimal balance = memBaseinfo.getBalance().add(oldTxns.getAmount());
        gameCommonService.updateUserBalance(memBaseinfo, oldTxns.getAmount(), GoldchangeEnum.REFUND, TradingEnum.INCOME);
        String dateStr = DateUtils.format(new Date(), DateUtils.ISO8601_DATE_FORMAT);

        Txns txns = new Txns();
        BeanUtils.copyProperties(oldTxns, txns);
        //游戏商注单号
        txns.setPlatformTxId(djCallBackParentReq.getTicket_id());
        //混合码
        txns.setBalance(balance);
        txns.setId(null);
        txns.setMethod("Cancel Bet");
        txns.setStatus("Running");
        txns.setCreateTime(dateStr);
        txnsMapper.insert(txns);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" ?>").append("<cancel_bet>>").append("<status_code>");
        stringBuilder.append(">00</status_code>").append("<status_text>OK</status_text>");
        stringBuilder.append("</cancel_bet>");
        return stringBuilder;
    }

}
