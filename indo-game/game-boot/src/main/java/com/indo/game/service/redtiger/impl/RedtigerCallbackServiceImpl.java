package com.indo.game.service.redtiger.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.redis.utils.GeneratorIdUtil;
import com.indo.common.utils.DateUtils;
import com.indo.game.mapper.TxnsMapper;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.game.pojo.entity.manage.GameCategory;
import com.indo.game.pojo.entity.manage.GameParentPlatform;
import com.indo.game.pojo.entity.manage.GamePlatform;
import com.indo.game.pojo.entity.manage.Txns;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.game.service.redtiger.RedtigerCallbackService;
import com.indo.user.pojo.bo.MemTradingBO;
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
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            String playerID = params.getString("userId");
            System.out.println("回调用户id:"+playerID);
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            // 查询玩家是否存在
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String uuid = params.getString("uuid");
            String sid = params.getString("sid");
            if (null == sid || "".equals(sid)) {
                CptOpenMember cptOpenMember = externalService.getCptOpenMember(playerID, OpenAPIProperties.REDTIGER_PLATFORM_CODE);
                if(null!=cptOpenMember) {
                    sid = cptOpenMember.getPassword();
                }else {
                    sid = uuid;
                }
            }
            JSONObject jsonObject1 = initSuccessResponse(uuid);
            jsonObject1.put("sid",sid);

            return jsonObject1;

        } catch (Exception e) {
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    @Override
    public Object check(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_check redtigerGame paramJson:{}, ip:{}", params, ip);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            // 查询玩家是否存在
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            JSONObject jsonObject1 = initSuccessResponse(params.getString("uuid"));
            jsonObject1.put("sid", params.get("sid"));

            return jsonObject1;

        } catch (Exception e) {
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    @Override
    public Object balance(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_balance redtigerGame paramJson:{}, ip:{}", params, ip);
        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            // 查询玩家是否存在
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            // 会员余额返回
            JSONObject jsonObject1 = initSuccessResponse(params.getString("uuid"));
            jsonObject1.put("balance", memBaseinfo.getBalance());
            jsonObject1.put("bonus", 0);
            return jsonObject1;
        } catch (Exception e) {
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    @Override
    public Object debit(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_debit  redtigerGame paramJson:{}, ip:{}", params, ip);

        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

//            JSONObject params = JSONObject.parseObject(String.valueOf(map));
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            JSONObject transaction = params.getJSONObject("transaction");
            String platformTxId = transaction.getString("id");
            JSONObject game = params.getJSONObject("game");
            JSONObject table = game.getJSONObject("details").getJSONObject("table");

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            GamePlatform gamePlatform;
            if("Y".equals(OpenAPIProperties.REDTIGER_IS_PLATFORM_LOGIN)){
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(platformGameParent.getPlatformCode(),platformGameParent.getPlatformCode());
            }else {
                gamePlatform = gameCommonService.getGamePlatformByplatformCodeAndParentName(table.getString("id"), platformGameParent.getPlatformCode());

            }
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());

            // 会员余额
            BigDecimal balance = memBaseinfo.getBalance();
            // 下注金额
            BigDecimal betAmount = transaction.getBigDecimal("amount");
            if (memBaseinfo.getBalance().compareTo(betAmount) < 0) {
                return initFailureResponse("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this bet.");
            }

            // 查询用户请求订单
            Txns oldTxns = getTxns(platformTxId, memBaseinfo.getAccount());
            if (null != oldTxns&&"Place Bet".equals(oldTxns.getMethod())) {
                return initFailureResponse("BET_ALREADY_EXIST", "订单已经存在");
            }

            // 下注金额小于0
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
                return initFailureResponse("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this bet.");
            }
            balance = balance.subtract(betAmount);
            // 更新玩家余额
            gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);

            Txns txns = new Txns();
            //游戏商注单号
            txns.setPlatformTxId(platformTxId);
            //此交易是否是投注 true是投注 false 否
            txns.setBet(true);
            //玩家 ID
            txns.setUserId(memBaseinfo.getAccount());
            //玩家货币代码
            txns.setCurrency(platformGameParent.getCurrencyType());
            txns.setGameInfo(game.getString("type"));
            txns.setRoundId(transaction.getString("refId"));
            //平台代码
            txns.setPlatform(platformGameParent.getPlatformCode());
            //平台名称
            txns.setPlatformEnName(platformGameParent.getPlatformEnName());
            txns.setPlatformCnName(platformGameParent.getPlatformCnName());
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
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
            txns.setWinningAmount(betAmount.negate());
            txns.setWinAmount(betAmount);
            //游戏平台的下注项目
            txns.setBetType(table.getString("id"));
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
//            txns.setWinningAmount(BigDecimal.ZERO);
            //玩家下注时间
            txns.setBetTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //真实下注金额,需增加在玩家的金额
            txns.setRealBetAmount(betAmount);
            //真实返还金额,游戏赢分
            txns.setRealWinAmount(BigDecimal.ZERO);
            //返还金额 (包含下注金额)
            //赌注的结果 : 赢:0,输:1,平手:2
            //有效投注金额 或 投注面值
            txns.setTurnover(betAmount);
            //辨认交易时间依据
            txns.setTxTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
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
            int num = txnsMapper.insert(txns);
            if (num <= 0) {
                return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
            }

            JSONObject jsonObject1 = initSuccessResponse(params.getString("uuid"));
            jsonObject1.put("balance", balance);
            jsonObject1.put("bonus", 0);
            return jsonObject1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    @Override
    public Object credit(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_credit redtigerGame paramJson:{}, ip:{}", params, ip);

        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String userId = params.getString("userId");
            if (null == userId || "".equals(userId)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            BigDecimal balance = memBaseinfo.getBalance();
            JSONObject transaction = params.getJSONObject("transaction");
            String platformTxId = transaction.getString("id");
            String refId = transaction.getString("refId");
            JSONObject game = params.getJSONObject("game");
            Txns oldTxns1 = getTxns(platformTxId, memBaseinfo.getAccount());
            if(null!=oldTxns1){
                return initFailureResponse("BET_ALREADY_EXIST", "订单已经存在");
            }
            // 查询用户请求订单
            Txns oldTxns = getTxnsByRoundId(refId, memBaseinfo.getAccount());
            if (null == oldTxns) {
                return initFailureResponse("BET_DOES_NOT_EXIST", "Please contact Customer Support for assistance.");
            }
            // 如果订单已经取消
            if ("Cancel Bet".equals(oldTxns.getMethod())) {
                return initFailureResponse("BET_ALREADY_EXIST", "Bet already exists in third party system.");
            }
            // 中奖金额
            BigDecimal betAmount = transaction.getBigDecimal("amount");
            // 中奖金额小于0
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
                return initFailureResponse("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this bet.");
            }
            if (betAmount.compareTo(BigDecimal.ZERO) != 0) {
                // 会员余额
                balance =balance.add(betAmount);
                // 更新玩家余额
                gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.SETTLE, TradingEnum.INCOME);
            }

            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxns, txns);
            //游戏商注单号
            txns.setPlatformTxId(platformTxId);
            txns.setBalance(balance);
            txns.setId(null);
            //更新时间
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
            txns.setWinningAmount(betAmount);
            txns.setWinAmount(betAmount);
            //赌注的结果 : 赢:0,输:1,平手:2
            int resultTyep;
            if (betAmount.compareTo(BigDecimal.ZERO) == 0) {
                resultTyep = 2;
            } else if (betAmount.compareTo(BigDecimal.ZERO) > 0) {
                resultTyep = 0;
            } else {
                resultTyep = 1;
            }
            oldTxns.setResultType(resultTyep);
            //操作名称
            txns.setMethod("Settle");
            txns.setStatus("Running");
            txns.setCreateTime(dateStr);
            txnsMapper.insert(txns);

            oldTxns.setStatus("Settle");
            oldTxns.setUpdateTime(dateStr);
            txnsMapper.updateById(oldTxns);

            JSONObject jsonObject1 = initSuccessResponse(params.getString("uuid"));
            jsonObject1.put("balance", balance);
            jsonObject1.put("bonus", 0);
            return jsonObject1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    @Override
    public Object cancel(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_cancel redtigerGame paramJson:{}, ip:{}", params, ip);


        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            JSONObject transaction = params.getJSONObject("transaction");
            String platformTxId = transaction.getString("id");
            String refId = transaction.getString("refId");
//            JSONObject game = params.getJSONObject("game");
//            JSONObject table = game.getJSONObject("details").getJSONObject("table");


            // 查询用户请求订单
            Txns oldTxns1 = getTxns(platformTxId, memBaseinfo.getAccount());
            if(null!=oldTxns1){
                return initFailureResponse("BET_ALREADY_EXIST", "订单已经存在");
            }
            // 查询用户请求订单
            Txns oldTxns = getTxnsByRoundId(refId, memBaseinfo.getAccount());
            if (null == oldTxns) {
                return initFailureResponse("BET_DOES_NOT_EXIST", "Please contact Customer Support for assistance.");
            }

            // 如果订单已经取消
            if ("Cancel Bet".equals(oldTxns.getMethod())) {
                return initFailureResponse("BET_ALREADY_EXIST", "Bet already exists in third party system.");
            }
            BigDecimal betAmount = transaction.getBigDecimal("amount");
            BigDecimal amount = oldTxns.getBetAmount();
            BigDecimal balance = memBaseinfo.getBalance();
            // 取消金额大于下注金额
            if (betAmount.compareTo(amount) == 1) {
                return initFailureResponse("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this bet.");
            }else {
                balance = balance.add(betAmount);
                gameCommonService.updateUserBalance(memBaseinfo, amount, GoldchangeEnum.REFUND, TradingEnum.INCOME);
            }

            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);

            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxns, txns);
            //游戏商注单号
            txns.setPlatformTxId(platformTxId);
            txns.setBalance(balance);
            txns.setId(null);
            txns.setStatus("Running");
            txns.setWinAmount(amount);
            txns.setRealWinAmount(amount);//真实返还金额
            txns.setMethod("Cancel Bet");
            txns.setCreateTime(dateStr);
            if (betAmount.compareTo(amount) == 1) {
                Txns txns2 = new Txns();
                BeanUtils.copyProperties(oldTxns, txns2);
                //游戏商注单号
                txns2.setPlatformTxId(platformTxId);
                txns2.setBalance(balance);
                txns2.setId(null);
                txns2.setStatus("Running");
                txns2.setWinningAmount(amount.subtract(betAmount).negate());
                txns2.setWinAmount(amount);
                txns2.setRealWinAmount(amount);//真实返还金额
                txns2.setMethod("Place Bet");
                txns2.setCreateTime(dateStr);
                txnsMapper.insert(txns2);
                txns.setStatus("Place Bet");
            }
            txnsMapper.insert(txns);

            oldTxns.setStatus("Cancel Bet");
            oldTxns.setUpdateTime(dateStr);
            txnsMapper.updateById(oldTxns);
            // 中奖金额小于0


            JSONObject jsonObject1 = initSuccessResponse(params.getString("uuid"));
            jsonObject1.put("balance", balance);
            jsonObject1.put("bonus", 0);
            return jsonObject1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    @Override
    public Object promo_payout(JSONObject params,String authToken , String ip) {
        logger.info("redtiger_promo_payout  redtigerGame paramJson:{}, ip:{}", params, ip);

        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            String playerID = params.getString("userId");
            if (null == playerID || "".equals(playerID)) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(playerID);
            if (null == memBaseinfo) {
                return initFailureResponse("INVALID_TOKEN_ID", "There has been a problem with the casino. User authentication failed or your session may be expired, please close the browser and try again. Error Code: EV01");
            }
            JSONObject promoTransaction = params.getJSONObject("promoTransaction");
            String platformTxId = promoTransaction.getString("id");
            JSONObject game = params.getJSONObject("game");
            JSONObject table = new JSONObject();
            if(null!=game){
                table = game.getJSONObject("details").getJSONObject("table");
            }

            // 赢奖金额
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
            // 免费回合游戏
            if ("FreeRoundPlayableSpent".equals(promoType)) {
                betAmount = promoTransaction.getBigDecimal("amount");
                roundId = promoTransaction.getString("voucherId");
            } else if ("JackpotWin".equals(promoType)) {
                // 免费回合头奖
                roundId = game.getJSONObject("details").getJSONObject("table").getString("id");
                promotionId = roundId;
                JSONArray jsonArray = promoTransaction.getJSONArray("jackpots");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    betAmount.add(jsonObject.getBigDecimal("winAmount"));
                }
            } else if ("RewardGamePlayableSpent".equals(promoType)) {
                // 由于花费了所有奖励游戏代金券可玩余额而发放了促销支出
                betAmount = promoTransaction.getBigDecimal("amount");
                roundId = promoTransaction.getString("voucherId");
            } else if ("RewardGameWinCapReached".equals(promoType)) {
                // 由于达到奖励游戏的最大可能奖金而发放了促销支出
                betAmount = promoTransaction.getBigDecimal("amount");
                roundId = promoTransaction.getString("voucherId");
            } else if ("RewardGameMinBetLimitReached".equals(promoType)) {

                // 由于奖励游戏可玩余额达到当前牌桌的最小下注限制，因此发放了促销奖金。
                betAmount = promoTransaction.getBigDecimal("amount");
                roundId = promoTransaction.getString("voucherId");
            } else if ("RtrMonetaryReward".equals(promoType)) {
                // 由于游戏回合中的某些配置事件而发放了促销支出。
                betAmount = promoTransaction.getBigDecimal("amount");
                roundId = promoTransaction.getString("bonusConfigId");
            } else if ("roulette".equals(promoType)) {

                // 因在游戏回合中获胜而发放促销奖金。
                betAmount = promoTransaction.getBigDecimal("amount");
                roundId = game.getJSONObject("details").getJSONObject("table").getString("id");
                promotionId = roundId;
            }

            // 赢奖金额小于0
            if (betAmount.compareTo(BigDecimal.ZERO) < 0) {
                return initFailureResponse("INSUFFICIENT_FUNDS", "You do not have sufficient funds to place this bet.");
            }

            // 查询用户请求订单
            Txns txns = getTxns(platformTxId, memBaseinfo.getAccount());
            if (null != txns) {
                return initFailureResponse("BET_DOES_NOT_EXIST", "Please contact Customer Support for assistance.");
            }
            txns = new Txns();
            // 会员余额
            BigDecimal balance = memBaseinfo.getBalance().add(betAmount);
            // 更新玩家余额
            gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.ACTIVITY_GIVE, TradingEnum.INCOME);

            //游戏商注单号
            txns.setPlatformTxId(platformTxId);
            //此交易是否是投注 true是投注 false 否
            txns.setBet(false);
            //玩家 ID
            txns.setUserId(memBaseinfo.getAccount());
            //玩家货币代码
            txns.setCurrency(params.getString("currency"));
            //游戏平台的下注项目
//            txns.setBetType(promoTransaction.getString("type"));
            // 奖金游戏
//            txns.setHasBonusGame(1);
            //平台代码
            txns.setPlatform(platformGameParent.getPlatformCode());
            //平台名称
            txns.setPlatformEnName(platformGameParent.getPlatformEnName());
            txns.setPlatformCnName(platformGameParent.getPlatformCnName());
            txns.setRoundId(roundId);
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
            txns.setBetAmount(BigDecimal.ZERO);
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
            txns.setWinningAmount(betAmount);
            //玩家下注时间
            txns.setBetTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //真实下注金额,需增加在玩家的金额
            txns.setRealBetAmount(BigDecimal.ZERO);
            //真实返还金额,游戏赢分
            txns.setRealWinAmount(betAmount);
            //返还金额 (包含下注金额)
            txns.setWinAmount(betAmount);
            // 活动派彩
            txns.setAmount(betAmount);
            // 活动ID
            txns.setPromotionId(promotionId);
            // 活动类型ID
            txns.setPromotionTypeId(promoType);
            //有效投注金额 或 投注面值
            txns.setTurnover(BigDecimal.ZERO);
            //辨认交易时间依据
            txns.setTxTime(DateUtils.formatByLong(System.currentTimeMillis(), DateUtils.newFormat));
            //操作名称
            txns.setMethod("Bonus");
            txns.setStatus("Running");
            //余额
            txns.setBalance(balance);
            //创建时间
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            txns.setCreateTime(dateStr);
            //投注 IP
            txns.setBetIp(ip);//  string 是 投注 IP
            int num = txnsMapper.insert(txns);
            if (num <= 0) {
                return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
            }

            JSONObject jsonObject1 = initSuccessResponse(params.getString("uuid"));
            jsonObject1.put("balance", balance);
            jsonObject1.put("bonus", 0);
            return jsonObject1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return initFailureResponse("UNKNOWN_ERROR","Please contact Customer Support for assistance.");
        }
    }

    /**
     * 获取历史订单
     *
     * @param reference 订单编号
     * @param userId    会员ID
     * @return 订单
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
     * 查询IP是否被封
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

    /**
     * 初始化成功json返回
     *
     * @return JSONObject
     */
    private JSONObject initSuccessResponse(String uuid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "OK");
        jsonObject.put("uuid", uuid);
        return jsonObject;
    }

    /**
     * 初始化交互失败返回
     *
     * @param error       错误码
     * @param description 错误描述
     * @return JSONObject
     */
    private JSONObject initFailureResponse(String error, String description) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", error);
        jsonObject.put("message", description);
        jsonObject.put("uuid", GeneratorIdUtil.generateId());
        return jsonObject;
    }

    private GameParentPlatform getGameParentPlatform() {
        return gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.REDTIGER_PLATFORM_CODE);
    }
}
