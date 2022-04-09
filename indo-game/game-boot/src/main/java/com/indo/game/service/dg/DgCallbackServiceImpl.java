package com.indo.game.service.dg;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.utils.DateUtils;
import com.indo.game.mapper.TxnsMapper;
import com.indo.game.pojo.dto.dg.DgCallBackReq;
import com.indo.game.pojo.entity.manage.Txns;
import com.indo.game.service.common.GameCommonService;
import com.indo.user.pojo.bo.MemTradingBO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;


/**
 * DG
 *
 * @author
 */
@Service
public class DgCallbackServiceImpl implements DgCallbackService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private GameCommonService gameCommonService;
    @Autowired
    private TxnsMapper txnsMapper;


    @Override
    public Object dgBalanceCallback(String agentName, DgCallBackReq dgCallBackReq, String ip) {
        JSONObject jsonObject = JSONObject.parseObject(dgCallBackReq.getMember());
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("username"));
        JSONObject dataJson = new JSONObject();
        if (null == memBaseinfo) {
            dataJson.put("codeId", "1");
            dataJson.put("token", dgCallBackReq.getToken());
            return dataJson;
        } else {
            dataJson.put("codeId", "0");
            dataJson.put("token", dgCallBackReq.getToken());
            JSONObject memberJson = new JSONObject();
            memberJson.put("username", jsonObject.getString("username"));
            memberJson.put("balance", memBaseinfo.getBalance());
            dataJson.put("member", memberJson);
            return dataJson;
        }
    }

    @Override
    public Object dgTransferCallback(DgCallBackReq dgCallBackReq, String ip, String agentName) {
        JSONObject jsonObject = JSONObject.parseObject(dgCallBackReq.getMember());
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("username"));
        JSONObject dataJson = new JSONObject();
        BigDecimal balance = memBaseinfo.getBalance();
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, dgCallBackReq.getData());
        wrapper.eq(Txns::getUserId, memBaseinfo.getId());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null != oldTxns) {
            dataJson.put("codeId", "98");
            dataJson.put("message", "操作失败！");
            return dataJson;
        }

        if (jsonObject.getBigDecimal("amount").compareTo(BigDecimal.ZERO) == 1) {//赢
            balance = balance.add(jsonObject.getBigDecimal("amount"));
            gameCommonService.updateUserBalance(memBaseinfo, jsonObject.getBigDecimal("amount"), GoldchangeEnum.DSFYXZZ, TradingEnum.INCOME);
        }
        if (jsonObject.getBigDecimal("amount").compareTo(BigDecimal.ZERO) == -1) {//输
            if (memBaseinfo.getBalance().compareTo(jsonObject.getBigDecimal("amount")) == -1) {
                dataJson.put("code", "120");
                dataJson.put("message", "余额不足");
                return dataJson;
            }
            balance = balance.subtract(jsonObject.getBigDecimal("amount"));
            gameCommonService.updateUserBalance(memBaseinfo, jsonObject.getBigDecimal("amount"), GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);
        }


        Txns txns = new Txns();
        //游戏商注单号
        txns.setPlatformTxId(dgCallBackReq.getData());
        //玩家 ID
        txns.setUserId(memBaseinfo.getId().toString());

        txns.setRoundId(dgCallBackReq.getTicketId());
        //平台代码
        txns.setPlatform("DG");
        //下注金额
        txns.setBetAmount(jsonObject.getBigDecimal("amount"));
        //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
        txns.setWinningAmount(jsonObject.getBigDecimal("amount"));
        //真实下注金额,需增加在玩家的金额
        txns.setRealBetAmount(jsonObject.getBigDecimal("amount"));
        //真实返还金额,游戏赢分
        txns.setRealWinAmount(jsonObject.getBigDecimal("amount"));
        //返还金额 (包含下注金额)
        //赌注的结果 : 赢:0,输:1,平手:2
        int resultTyep;
        //有效投注金额 或 投注面值
        txns.setTurnover(jsonObject.getBigDecimal("amount"));
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
        int num = txnsMapper.insert(txns);
        if (num <= 0) {
            dataJson.put("codeId", "98");
            dataJson.put("message", "操作失败！");
            return dataJson;
        }
        dataJson.put("codeId", "0");
        dataJson.put("token", dgCallBackReq.getToken());
        dataJson.put("data", dgCallBackReq.getData());
        JSONObject memberJson = new JSONObject();
        memberJson.put("username", jsonObject.getString("username"));
        memberJson.put("amount", jsonObject.getBigDecimal("amount"));
        memberJson.put("balance", balance);
        dataJson.put("member", memberJson);
        return dataJson;
    }

    @Override
    public Object dgCheckTransferCallback(DgCallBackReq dgCallBackReq, String ip, String agentName) {
        JSONObject dataJson = new JSONObject();
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, dgCallBackReq.getData());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (null == oldTxns) {
            dataJson.put("codeId", "98");
            dataJson.put("message", "操作失败！");
            return dataJson;
        }
        dataJson.put("codeId", "0");
        dataJson.put("token", dgCallBackReq.getToken());
        return dataJson;
    }

    @Override
    public Object djInformCallback(DgCallBackReq dgCallBackReq, String ip, String agentName) {
        JSONObject jsonObject = JSONObject.parseObject(dgCallBackReq.getMember());
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("username"));
        JSONObject dataJson = new JSONObject();
        BigDecimal balance = memBaseinfo.getBalance();
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, dgCallBackReq.getData());
        wrapper.eq(Txns::getUserId, memBaseinfo.getId());
        Txns oldTxns = txnsMapper.selectOne(wrapper);
        if (jsonObject.getBigDecimal("amount").compareTo(BigDecimal.ZERO) == -1) {
            if (null != oldTxns) {
                balance = balance.add(jsonObject.getBigDecimal("amount"));
                gameCommonService.updateUserBalance(memBaseinfo, jsonObject.getBigDecimal("amount"), GoldchangeEnum.DSFYXZZ, TradingEnum.INCOME);
            } else {
                dataJson.put("codeId", "0");
                dataJson.put("token", dgCallBackReq.getToken());
                dataJson.put("data", dgCallBackReq.getData());
                JSONObject memberJson = new JSONObject();
                memberJson.put("username", jsonObject.getString("username"));
                memberJson.put("balance", balance);
                dataJson.put("member", memberJson);
                return dataJson;
            }
        } else if (jsonObject.getBigDecimal("amount").compareTo(BigDecimal.ZERO) == 1) {
            if (null == oldTxns) {
                balance = balance.add(jsonObject.getBigDecimal("amount"));
                gameCommonService.updateUserBalance(memBaseinfo, jsonObject.getBigDecimal("amount"), GoldchangeEnum.DSFYXZZ, TradingEnum.INCOME);
            } else {
                dataJson.put("codeId", "0");
                dataJson.put("data", dgCallBackReq.getData());
                dataJson.put("token", dgCallBackReq.getToken());
                JSONObject memberJson = new JSONObject();
                memberJson.put("balance", balance);
                memberJson.put("username", jsonObject.getString("username"));
                dataJson.put("member", memberJson);
                return dataJson;
            }
        }
        Txns txns = new Txns();
        BeanUtils.copyProperties(oldTxns, txns);
        txns.setId(null);
        txns.setBalance(balance);
        String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
        txns.setCreateTime(dateStr);
        txnsMapper.insert(txns);
        oldTxns.setStatus("Cancel");
        oldTxns.setUpdateTime(dateStr);
        txnsMapper.updateById(oldTxns);

        dataJson.put("codeId", "0");
        dataJson.put("token", dgCallBackReq.getToken());
        dataJson.put("data", dgCallBackReq.getData());
        JSONObject memberJson = new JSONObject();
        memberJson.put("balance", balance);
        memberJson.put("username", jsonObject.getString("username"));
        dataJson.put("member", memberJson);
        return dataJson;
    }

    @Override
    public Object mgOrderCallback(DgCallBackReq dgCallBackReq, String ip, String agentName) {
        JSONObject jsonObject = JSONObject.parseObject(dgCallBackReq.getMember());
        JSONObject dataJson = new JSONObject();
        MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(jsonObject.getString("username"));
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Txns::getRoundId, dgCallBackReq.getTicketId());
        wrapper.eq(Txns::getUserId, memBaseinfo.getId());
        List<Txns> oldTxns = txnsMapper.selectList(wrapper);
        JSONArray jsonArray = new JSONArray();
        if (oldTxns.size() > 0) {
            for (Txns txns : oldTxns) {
                JSONObject json = new JSONObject();
                json.put("username", memBaseinfo.getAccount());
                json.put("ticketId", dgCallBackReq.getTicketId());
                json.put("serial", txns.getPlatformTxId());
                json.put("amount", txns.getBetAmount());
                jsonArray.add(json);
            }
            dataJson.put("codeId", "0");
            dataJson.put("token", dgCallBackReq.getToken());
            dataJson.put("ticketId", dgCallBackReq.getTicketId());
            dataJson.put("list", jsonArray);
            return dataJson;
        }
        dataJson.put("codeId", "0");
        dataJson.put("token", dgCallBackReq.getToken());
        dataJson.put("ticketId", dgCallBackReq.getTicketId());
        dataJson.put("list", jsonArray);
        return dataJson;
    }

    @Override
    public Object mgUnsettleCallback(DgCallBackReq dgCallBackReq, String ip, String agentName) {
        JSONObject dataJson = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        dataJson.put("codeId", "0");
        dataJson.put("token", dgCallBackReq.getToken());
        dataJson.put("list", jsonArray);
        return dataJson;
    }
}
