package com.indo.game.service.km.impl;

import com.alibaba.fastjson.JSONArray;
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
import com.indo.game.service.km.KmCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * kingmaker
 *
 * @author
 */
@Service
public class KmCallbackServiceImpl implements KmCallbackService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private GameCommonService gameCommonService;
    @Autowired
    private TxnsMapper txnsMapper;


    @Override
    public Object kmBalanceCallback(JSONObject jsonObject, String ip) {
        JSONArray array =  jsonObject.getJSONArray("users");
        JSONArray jsonArray = new JSONArray();
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.KM_PLATFORM_CODE);
        if (array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                JSONObject json = array.getJSONObject(i);
                MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(json.getString("userid"));
                JSONObject dataJson = new JSONObject();
                if (null == memBaseinfo) {
                    dataJson.put("err", "50");
                    dataJson.put("errdesc", "Currency Mismatch");
                    jsonArray.add(dataJson);
                } else {
                    JSONArray arrayList = new JSONArray();
                    JSONObject object = new JSONObject();
                    object.put("code", json.getString("walletcode"));
                    object.put("bal", memBaseinfo.getBalance().divide(gameParentPlatform.getCurrencyPro()));
                    object.put("cur", json.getString("cur"));
                    object.put("name", memBaseinfo.getAccount());
                    object.put("desc", memBaseinfo.getBalance().divide(gameParentPlatform.getCurrencyPro()));
                    arrayList.add(object);
                    dataJson.put("userid", json.getString("userid"));
                    dataJson.put("wallets", arrayList);
                    jsonArray.add(dataJson);
                }
            }
        }
        JSONObject  json = new JSONObject();
        json.put("users", jsonArray);
        return json;
    }

    @Override
    public Object kmDebitCallback(JSONObject jsonObject, String ip) {
        JSONArray array = jsonObject.getJSONArray("transactions");
        JSONArray jsonArray = new JSONArray();
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.KM_PLATFORM_CODE);
        if (array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                JSONObject json = array.getJSONObject(i);
                logger.info("kmDebitCallback json=="+i+":{}", JSONObject.toJSONString(json));
                BigDecimal amt = null!=json.getBigDecimal("amt")?json.getBigDecimal("amt").multiply(gameParentPlatform.getCurrencyPro()):BigDecimal.ZERO;
                if(null==amt){
                    amt = BigDecimal.ZERO;
                }
                MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(json.getString("userid"));
                BigDecimal balance = memBaseinfo.getBalance();
                JSONObject dataJson = new JSONObject();
                if (null == memBaseinfo) {
                    dataJson.put("err", " 10");
                    dataJson.put("errdesc", "Token has expired");
                    jsonArray.add(dataJson);
                }

                GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(OpenAPIProperties.KM_PLATFORM_CODE,gameParentPlatform.getPlatformCode());;
                GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
                if ("Gao_Gae".equals(json.getString("gamecode")) || "Kingmaker_Pok_Deng".equals(json.getString("gamecode"))
                        || "Pai_Kang".equals(json.getString("gamecode")) || "Blackjack".equals(json.getString("gamecode"))
                        || "Teen_Patti".equals(json.getString("gamecode")) || "Five_Card_Poker".equals(json.getString("gamecode"))) {
                    if ("500".equals(json.getString("txtype")) || "530".equals(json.getString("txtype")) || "540".equals(json.getString("txtype"))) {
                        dataJson.put("txid", json.getString("ptxid"));
                        dataJson.put("ptxid", json.getString("ptxid"));
                        dataJson.put("bal", balance.divide(gameParentPlatform.getCurrencyPro()));
                        dataJson.put("cur", json.getString("cur"));
                        dataJson.put("dup", "false");
                        jsonArray.add(dataJson);
                        continue;
                    } else {
                        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
                        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Adjust Bet"));
                        wrapper.eq(Txns::getStatus, "Running");
                        wrapper.eq(Txns::getPlatformTxId, json.getString("ptxid"));
                        wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
//                        wrapper.eq(Txns::getUserId, memBaseinfo.getId());
                        Txns oldTxns = txnsMapper.selectOne(wrapper);
                        if (null != oldTxns) {
                            dataJson.put("txid", json.getString("ptxid"));
                            dataJson.put("ptxid", json.getString("ptxid"));
                            dataJson.put("dup", "true");
                            jsonArray.add(dataJson);
                            continue;
                        }

                        if (memBaseinfo.getBalance().compareTo(amt) == -1) {
                            dataJson.put("err", 10);
                            dataJson.put("errdesc", "Token has expired");
                            jsonArray.add(dataJson);
                            continue;
                        }
                        if (amt.compareTo(BigDecimal.ZERO) != 0) {
                            balance = balance.subtract(amt);
                            gameCommonService.updateUserBalance(memBaseinfo, amt, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);
                        }
                    }

                } else {
                    if ("530".equals(json.getString("txtype")) || "540".equals(json.getString("txtype"))) {
                        dataJson.put("txid", json.getString("ptxid"));
                        dataJson.put("bal", balance);
                        dataJson.put("ptxid", json.getString("ptxid"));
                        dataJson.put("cur", json.getString("cur"));
                        dataJson.put("dup", "false");
                        jsonArray.add(dataJson);
                        continue;
                    }
                    if (memBaseinfo.getBalance().compareTo(amt) == -1) {
                        dataJson.put("err", 10);
                        dataJson.put("errdesc", "Token has expired");
                        jsonArray.add(dataJson);
                        continue;
                    }
                    if (amt.compareTo(BigDecimal.ZERO) != 0) {
                        balance = balance.subtract(amt);
                        gameCommonService.updateUserBalance(memBaseinfo, amt, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);
                    }
                }

                Txns txns = new Txns();
                //??????????????????
                txns.setPlatformTxId(json.getString("ptxid"));
                //?????? ID
                txns.setUserId(memBaseinfo.getAccount());
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

                txns.setRoundId(json.getString("roundid"));

                txns.setMpId(json.getInteger("txtype"));
                //????????????
                txns.setPlatform("DG");
                //????????????
                txns.setBetAmount(amt);
                //???????????????????????????????????????????????????0??????????????????
                txns.setWinningAmount(amt.negate());
                txns.setWinAmount(amt);
                //??????????????????,???????????????????????????
                txns.setRealBetAmount(amt);
                //??????????????????,????????????
                txns.setRealWinAmount(amt);
                //???????????? (??????????????????)
                //??????????????? : ???:0,???:1,??????:2
                int resultTyep;
                //?????????????????? ??? ????????????
                txns.setTurnover(amt);
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
                    dataJson.put("txid", json.getString("ptxid"));
                    dataJson.put("bal", balance.divide(gameParentPlatform.getCurrencyPro()));
                    dataJson.put("ptxid", json.getString("ptxid"));
                    dataJson.put("cur", json.getString("cur"));
                    dataJson.put("dup", "false");
                    jsonArray.add(dataJson);
                    continue;
                }

                dataJson.put("txid", json.getString("ptxid"));
                dataJson.put("bal", balance);
                dataJson.put("ptxid", json.getString("ptxid"));
                dataJson.put("cur", json.getString("cur"));
                dataJson.put("dup", "false");
                jsonArray.add(dataJson);
            }
        }
        JSONObject object = new JSONObject();
        object.put("transactions", jsonArray);
        return object;
    }

    @Override
    public Object kmCreditCallback(JSONObject jsonObject, String ip) {
        JSONArray array = jsonObject.getJSONArray("transactions");
        JSONArray jsonArray = new JSONArray();
        GameParentPlatform gameParentPlatform = gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.KM_PLATFORM_CODE);
        if (array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                Txns oldTxns = new Txns();
                JSONObject json = array.getJSONObject(i);
                logger.info("kmCreditCallback json=="+i+":{}", JSONObject.toJSONString(json));
                BigDecimal amt = null!=json.getBigDecimal("amt")?json.getBigDecimal("amt").multiply(gameParentPlatform.getCurrencyPro()):BigDecimal.ZERO;
                if(null==amt){
                    amt = BigDecimal.ZERO;
                }
                MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(json.getString("userid"));
                BigDecimal balance = memBaseinfo.getBalance();
                JSONObject dataJson = new JSONObject();
                if (null == memBaseinfo) {
                    dataJson.put("err", " 10");
                    dataJson.put("errdesc", "Token has expired");
                    jsonArray.add(dataJson);
                }

                GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(OpenAPIProperties.KM_PLATFORM_CODE,gameParentPlatform.getPlatformCode());;
                GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());
                if ("Gao_Gae".equals(json.getString("gamecode")) || "Kingmaker_Pok_Deng".equals(json.getString("gamecode"))
                        || "Pai_Kang".equals(json.getString("gamecode")) || "Blackjack".equals(json.getString("gamecode"))
                        || "Teen_Patti".equals(json.getString("gamecode")) || "Five_Card_Poker".equals(json.getString("gamecode"))) {
                    if ("510".equals(json.getString("txtype")) || "540".equals(json.getString("txtype"))) {
                        dataJson.put("txid", json.getString("ptxid"));
                        dataJson.put("ptxid", json.getString("ptxid"));
                        dataJson.put("bal", balance.divide(gameParentPlatform.getCurrencyPro()));
                        dataJson.put("cur", json.getString("cur"));
                        dataJson.put("dup", "false");
                        jsonArray.add(dataJson);
                        continue;
                    } else if ("520".equals(json.getString("txtype")) ) {
                        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
                        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Settle"));
                        wrapper.eq(Txns::getStatus, "Running");
                        wrapper.eq(Txns::getRoundId, json.getString("roundid"));
                        wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
//                        wrapper.eq(Txns::getUserId, memBaseinfo.getId());
                        List<Txns> oldTxnsList = txnsMapper.selectList(wrapper);
                        if (oldTxnsList.size() <= 0) {
                            dataJson.put("err", " 10");
                            dataJson.put("errdesc", "Token has expired");
                            jsonArray.add(dataJson);
                        }
                        BigDecimal addBigDecimal = BigDecimal.ZERO;
                        BigDecimal betBigDecimal = BigDecimal.ZERO;
                        for (Txns txns : oldTxnsList) {
                            if (txns.getMpId() == 610) {
                                addBigDecimal = txns.getBetAmount();
                            }
                            if (txns.getMpId() == 500) {
                                betBigDecimal = txns.getBetAmount();
                            }
                        }
                        if (amt.compareTo(betBigDecimal) == -1) {
                            BigDecimal money = addBigDecimal.subtract(betBigDecimal);
                            balance = balance.add(money);
                            gameCommonService.updateUserBalance(memBaseinfo, money, GoldchangeEnum.REFUND, TradingEnum.INCOME);
                        }
                    } else {
                        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
                        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet").or().eq(Txns::getMethod, "Cancel Bet").or().eq(Txns::getMethod, "Settle"));
                        wrapper.eq(Txns::getStatus, "Running");
                        wrapper.eq(Txns::getPlatformTxId, json.getString("ptxid"));
                        wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
//                        wrapper.eq(Txns::getUserId, memBaseinfo.getId());
                        oldTxns = txnsMapper.selectOne(wrapper);
                        if (null != oldTxns) {
                            dataJson.put("txid", json.getString("ptxid"));
                            dataJson.put("ptxid", json.getString("ptxid"));
                            dataJson.put("dup", "true");
                            jsonArray.add(dataJson);
                            continue;
                        }
                        if (amt.compareTo(BigDecimal.ZERO) != 0) {
                            balance = balance.add(amt);
                            gameCommonService.updateUserBalance(memBaseinfo, amt, GoldchangeEnum.DSFYXZZ, TradingEnum.INCOME);
                        }
                    }

                } else {
                    if ("530".equals(json.getString("txtype")) || "540".equals(json.getString("txtype"))) {
                        dataJson.put("txid", json.getString("ptxid"));
                        dataJson.put("bal", balance.divide(gameParentPlatform.getCurrencyPro()));
                        dataJson.put("ptxid", json.getString("ptxid"));
                        dataJson.put("cur", json.getString("cur"));
                        dataJson.put("dup", "false");
                        jsonArray.add(dataJson);
                        continue;
                    }
                    if (amt.compareTo(BigDecimal.ZERO) != 0) {
                        balance = balance.add(amt);
                        gameCommonService.updateUserBalance(memBaseinfo, amt, GoldchangeEnum.DSFYXZZ, TradingEnum.INCOME);
                    }
                }
//????????????
                String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
                Txns txns = new Txns();
                if(null!=oldTxns){
                    oldTxns.setStatus("Settle");
                    oldTxns.setUpdateTime(dateStr);
                    txnsMapper.updateById(oldTxns);
                    BeanUtils.copyProperties(oldTxns, txns);
                    txns.setId(null);
                }
                //??????????????????
                txns.setPlatformTxId(json.getString("ptxid"));
                //?????? ID
                txns.setUserId(memBaseinfo.getId().toString());

                txns.setRoundId(json.getString("roundid"));
                //????????????
                txns.setPlatform("KingMaker");
                //????????????
                txns.setBetAmount(amt);
                //???????????????????????????????????????????????????0??????????????????
                txns.setWinningAmount(amt);
                txns.setWinAmount(amt);
                //??????????????????,???????????????????????????
                txns.setRealBetAmount(amt);
                //??????????????????,????????????
                txns.setRealWinAmount(amt);
                //???????????? (??????????????????)
                //??????????????? : ???:0,???:1,??????:2
                int resultTyep;
                //?????????????????? ??? ????????????
                txns.setTurnover(amt);
                //????????????
                txns.setMethod("Settle");
                txns.setStatus("Running");
                //??????
                txns.setBalance(balance);

                txns.setCreateTime(dateStr);
                //?????? IP
                txns.setBetIp(ip);//  string ??? ?????? IP
                int num = txnsMapper.insert(txns);
                if (num <= 0) {
                    dataJson.put("txid", json.getString("ptxid"));
                    dataJson.put("bal", balance.divide(gameParentPlatform.getCurrencyPro()));
                    dataJson.put("ptxid", json.getString("ptxid"));
                    dataJson.put("cur", json.getString("cur"));
                    dataJson.put("dup", "false");
                    jsonArray.add(dataJson);
                    continue;
                }

                dataJson.put("txid", json.getString("ptxid"));
                dataJson.put("bal", balance);
                dataJson.put("ptxid", json.getString("ptxid"));
                dataJson.put("cur", json.getString("cur"));
                dataJson.put("dup", "false");
                jsonArray.add(dataJson);
            }
        }
        JSONObject object = new JSONObject();
        object.put("transactions", jsonArray);
        return object;
    }
}


