package com.indo.game.service.bti.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.utils.DateUtils;
import com.indo.common.utils.StringUtils;
import com.indo.game.mapper.TxnsMapper;
import com.indo.game.pojo.dto.bti.BtiBetRequest;
import com.indo.game.pojo.dto.bti.BtiCreditRequest;
import com.indo.game.pojo.dto.bti.BtiReserveBetsRequest;
import com.indo.game.pojo.entity.CptOpenMember;
import com.indo.game.pojo.entity.manage.GameCategory;
import com.indo.game.pojo.entity.manage.GameParentPlatform;
import com.indo.game.pojo.entity.manage.GamePlatform;
import com.indo.game.pojo.entity.manage.Txns;
import com.indo.game.service.bti.BtiCallbackService;
import com.indo.game.service.common.GameCommonService;
import com.indo.game.service.cptopenmember.CptOpenMemberService;
import com.indo.user.pojo.bo.MemTradingBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BtiCallbackServiceImpl implements BtiCallbackService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private GameCommonService gameCommonService;
    @Autowired
    private CptOpenMemberService externalService;
    @Autowired
    private TxnsMapper txnsMapper;

    @Override
    public Object validateToken(String authToken, String ip) {
        logger.info("bti_validateToken btiGame paramJson:{}, ip:{}", authToken, ip);
        try {

            GameParentPlatform gameParentPlatform = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, gameParentPlatform)) {
                return initFailureResponse(-6, "非信任來源IP");
            }

            CptOpenMember cptOpenMember = externalService.quertCptOpenMember(authToken, gameParentPlatform.getPlatformCode());

            if (null == cptOpenMember) {
                return initFailureResponse(-3, "token 错误");
            }

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(cptOpenMember.getUserName());
            if (null == memBaseinfo) {
                return initFailureResponse(-2, "用户不存在");
            }

            JSONObject respJson = initSuccessResponse();
            respJson.put("cust_id", memBaseinfo.getAccount());
            respJson.put("cust_login", memBaseinfo.getAccount());
            respJson.put("city", "");
            respJson.put("country", "");
            respJson.put("currency_code", gameParentPlatform.getCurrencyType());
            respJson.put("balance", memBaseinfo.getBalance());

            return respJson;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(-1, e.getMessage());
        }
    }

    @Override
    public Object reserve(BtiReserveBetsRequest reserveBetsRequest, String ip) {
        logger.info("bti_reserve btiGame paramJson:{}, ip:{}", JSONObject.toJSONString(reserveBetsRequest), ip);

        try {

            GameParentPlatform gameParentPlatform = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, gameParentPlatform)) {
                return initFailureResponse(-6, "非信任來源IP");
            }

            String paySerialno = reserveBetsRequest.getReserveId();
            String userName = reserveBetsRequest.getCustId();
            BigDecimal betAmount = reserveBetsRequest.getAmount();

            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(userName);
            if (null == memBaseinfo) {
                return initFailureResponse(-2, "用户不存在");
            }
            BigDecimal balance = memBaseinfo.getBalance();
            if (balance.compareTo(betAmount) < 0) {
                return initFailureResponse(-10, "玩家余额不足");
            }
            // 查询订单
            Txns oldTxns = getTxns(gameParentPlatform, paySerialno, null);
            // 重复订单
            if (null != oldTxns) {
                return initFailureResponse(-10, "该注单已承认");
            }
            GamePlatform gamePlatform = gameCommonService.getGamePlatformByplatformCode(reserveBetsRequest.getPlatform());
            if (null == gamePlatform) {
                return initFailureResponse(-10, "游戏不存在");
            }
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());

            balance = balance.subtract(betAmount);
            // 更新余额
            gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.PLACE_BET, TradingEnum.SPENDING);

            List<BtiBetRequest> list = reserveBetsRequest.getBetList();
            String betTypeId = null;
            BigDecimal odds = BigDecimal.ZERO;

            for (BtiBetRequest item : list) {
                odds = BigDecimal.valueOf(item.getOdds());
                betTypeId = item.getBetTypeID();
            }

            Txns txns = new Txns();
            //游戏商注单号
            txns.setPlatformTxId(paySerialno);

            //玩家 ID
            txns.setUserId(memBaseinfo.getId().toString());
            //玩家货币代码
            txns.setCurrency(gameParentPlatform.getCurrencyType());
            txns.setOdds(odds);

            // lineid拼接写入游戏信息中
            txns.setGameInfo(list.stream().map(a -> a.getBetID()).collect(Collectors.joining(",")));
            //平台代码
            txns.setPlatform(gameParentPlatform.getPlatformCode());
            //平台名称
            txns.setPlatformEnName(gameParentPlatform.getPlatformEnName());
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
            //游戏平台的下注项目
            txns.setBetType(betTypeId);
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
//            txns.setWinningAmount(winloseAmount);
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            //玩家下注时间
            txns.setBetTime(dateStr);
            //真实下注金额,需增加在玩家的金额
            txns.setRealBetAmount(betAmount);
            //真实返还金额,游戏赢分
//            txns.setRealWinAmount(winloseAmount);
            //此交易是否是投注 true是投注 false 否
            txns.setBet(true);
            //返还金额 (包含下注金额)
//            txns.setWinAmount(winloseAmount);
            //有效投注金额 或 投注面值
            txns.setTurnover(betAmount);
            //辨认交易时间依据
            txns.setTxTime(dateStr);
            //操作名称
            txns.setMethod("Place Bet");
            txns.setStatus("Running");
            //余额
            txns.setBalance(balance);
            //创建时间

            txns.setCreateTime(dateStr);
            //投注 IP
            txns.setBetIp(ip);//  string 是 投注 IP
            int num = txnsMapper.insert(txns);
            if (num <= 0) {
                int count = 0;
                // 失败重试
                while (count < 5) {
                    num = txnsMapper.insert(txns);
                    if (num > 0) break;
                    count++;
                }
            }

            JSONObject jsonObject = initSuccessResponse();
            jsonObject.put("trx_id", paySerialno);
            jsonObject.put("balance", balance);
            jsonObject.put("BonusUsed", BigDecimal.ZERO);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(-1, e.getMessage());
        }
    }

    @Override
    public Object debitReserve(BtiReserveBetsRequest reserveBetsRequest, String ip, String reqId) {
        logger.info("bti_debitReserve btiGame paramJson:{}, ip:{}", JSONObject.toJSONString(reserveBetsRequest), ip);

        try {

            GameParentPlatform gameParentPlatform = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, gameParentPlatform)) {
                return initFailureResponse(0, "非信任來源IP");
            }

            String paySerialno = reserveBetsRequest.getReserveId();
            String userName = reserveBetsRequest.getCustId();

            // 查询订单
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(userName);
            if (null == memBaseinfo) {
                return initFailureResponse(0, "用户不存在");
            }
            BigDecimal balance = memBaseinfo.getBalance();
            // reserve_id 查询是否存在
            Txns oldTxnsReserve = getTxns(gameParentPlatform, paySerialno, null);
            if (null == oldTxnsReserve) {
                return initFailureResponse(0, "reserve_id not found");
            }

            // req_id 查询是否存在
            Txns oldReqTxnsReserve = getTxns(gameParentPlatform, paySerialno, reqId);
            if (null != oldReqTxnsReserve) {
                return initFailureResponse(0, "req_id is already debit");
            }

            // // 写入新的debit订单，用于后续commit进行下注金额比对
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxnsReserve, txns);
            txns.setRePlatformTxId(reqId);
            txns.setBalance(balance);
            txns.setId(null);
            txns.setTurnover(reserveBetsRequest.getAmount());
            txns.setBetAmount(reserveBetsRequest.getAmount());
            txns.setRealBetAmount(reserveBetsRequest.getAmount());
            txns.setMethod("Place Bet");
            txns.setCreateTime(dateStr);
            txnsMapper.insert(txns);

            JSONObject jsonObject = initSuccessResponse();
            jsonObject.put("trx_id", reqId);
            jsonObject.put("balance", balance);
            jsonObject.put("BonusUsed", BigDecimal.ZERO);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(0, e.getMessage());
        }
    }

    @Override
    public Object cancelReserve(BtiReserveBetsRequest reserveBetsRequest, String ip) {
        logger.info("bti_cancelReserve btiGame paramJson:{}, ip:{}", JSONObject.toJSONString(reserveBetsRequest), ip);

        try {
            GameParentPlatform platformGameParent = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, platformGameParent)) {
                return initFailureResponse(-6, "非信任來源IP");
            }

            String paySerialno = reserveBetsRequest.getReserveId();
            String userName = reserveBetsRequest.getCustId();
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(userName);
            if (null == memBaseinfo) {
                return initFailureResponse(-2, "用户不存在");
            }

            // 查询用户请求订单
            Txns oldTxns = getTxns(platformGameParent, paySerialno, null);
            if (null == oldTxns) {
                JSONObject json = initSuccessResponse();
                json.put("error_message", "Reserve was not found");
                json.put("balance", memBaseinfo.getBalance());
                return json;
            }

            // 如果订单已经取消
            if ("Cancel Bet".equals(oldTxns.getMethod())) {
                return initFailureResponse(-10, "注单已取消");
            }

            // 如果订单有已经结算的
            if (null != getTxnsHasDebit(platformGameParent, paySerialno)) {
                return initFailureResponse(-10, "注单已结算");
            }

            // 回退金额（预扣款注单下注金额）
            BigDecimal winAmount = oldTxns.getAmount();

            BigDecimal balance = memBaseinfo.getBalance().add(winAmount);
            // 会员退款
            gameCommonService.updateUserBalance(memBaseinfo, winAmount, GoldchangeEnum.CANCEL_BET, TradingEnum.INCOME);
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);

            Txns txns = new Txns();
            BeanUtils.copyProperties(oldTxns, txns);
            txns.setBalance(balance);
            txns.setId(null);
            txns.setStatus("Running");
            txns.setRealWinAmount(winAmount);//真实返还金额
            txns.setMethod("Cancel Bet");
            txns.setCreateTime(dateStr);
            txnsMapper.insert(txns);
            // 更新预扣注单状态
            update(platformGameParent.getPlatformCode(), paySerialno, "Adjust");

            // 构建返回
            JSONObject json = initSuccessResponse();
            json.put("balance", balance);
            return json;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(-1, e.getMessage());
        }
    }

    @Override
    public Object commitReserve(BtiReserveBetsRequest reserveBetsRequest, String ip) {
        logger.info("bti_commitReserve  btiGame paramJson:{}, ip:{}", JSONObject.toJSONString(reserveBetsRequest), ip);

        try {

            GameParentPlatform gameParentPlatform = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, gameParentPlatform)) {
                return initFailureResponse(0, "非信任來源IP");
            }

            String paySerialno = reserveBetsRequest.getReserveId();
            String userName = reserveBetsRequest.getCustId();

            // 查询订单
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(userName);
            if (null == memBaseinfo) {
                return initFailureResponse(0, "用户不存在");
            }
            // reserve_id 查询是否存在
            Txns oldTxnsReserve = getReserveTxns(gameParentPlatform, paySerialno);
            if (null == oldTxnsReserve) {
                return initFailureResponse(0, "reserve_id not found");
            }

            // 预扣款金额
            BigDecimal betAmount = oldTxnsReserve.getBetAmount();

            // 预扣订单下数据
            List<Txns> txnslist = getTxnsHasDebit(gameParentPlatform, paySerialno);
            // 提交debit注单金额总和
            BigDecimal debitAmount = BigDecimal.valueOf(txnslist.stream().mapToDouble(o -> o.getBetAmount().doubleValue()).sum());
            BigDecimal balance = memBaseinfo.getBalance();
            // 预扣款金额比下注金额多， 需要回退用户金额
            if (betAmount.compareTo(debitAmount) > 0) {
                // 回退部分预扣金额
                gameCommonService.updateUserBalance(memBaseinfo, betAmount.subtract(debitAmount), GoldchangeEnum.UNVOID_SETTLE, TradingEnum.INCOME);
                balance = balance.add(betAmount.subtract(debitAmount));

            }

            // 更新预扣注单状态
            update(gameParentPlatform.getPlatformCode(), paySerialno, "Settle");

            JSONObject jsonObject = initSuccessResponse();
            jsonObject.put("balance", balance);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(0, e.getMessage());
        }
    }

    @Override
    public Object debitCustomer(BtiCreditRequest btiCreditRequest, String ip, String reqId) {
        logger.info("bti_debitCustomer btiGame paramJson:{}, ip:{}", JSONObject.toJSONString(btiCreditRequest), ip);

        try {

            GameParentPlatform gameParentPlatform = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, gameParentPlatform)) {
                return initFailureResponse(0, "非信任來源IP");
            }

            String userName = btiCreditRequest.getCustomerID();

            // 查询订单
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(userName);
            if (null == memBaseinfo) {
                return initFailureResponse(0, "用户不存在");
            }

            Txns oldTxns = getTxns(gameParentPlatform, reqId, null);
            // 重复订单
            if (null != oldTxns) {
                return initFailureResponse(0, "注单已结算");
            }
            // 扣款
            BigDecimal betAmount = btiCreditRequest.getAmount();
            BigDecimal balance = memBaseinfo.getBalance();

            GamePlatform gamePlatform = gameCommonService.getGamePlatformByParentName(OpenAPIProperties.BTI_PLATFORM_CODE).get(0);
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());


            balance = balance.subtract(betAmount);
            // 更新余额
            gameCommonService.updateUserBalance(memBaseinfo, betAmount, GoldchangeEnum.BETNSETTLE, TradingEnum.SPENDING);


            Txns txns = new Txns();
            //游戏商注单号
            txns.setPlatformTxId(reqId);

            //玩家 ID
            txns.setUserId(memBaseinfo.getId().toString());
            //玩家货币代码
            txns.setCurrency(gameParentPlatform.getCurrencyType());
//            txns.setOdds(kaCallbackPlayReq.getBetPerSelection());
//            txns.setRoundId(cmdCallbackBetReq.getRound());
//            txns.setGameInfo(item.getBtiSelectionsRequest().getSelectionList().stream()
//                    .map(BtiCreditSelectionRequest::getLineID).collect(Collectors.joining(",")));
            //平台代码
            txns.setPlatform(gameParentPlatform.getPlatformCode());
            //平台名称
            txns.setPlatformEnName(gameParentPlatform.getPlatformEnName());
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
            //游戏平台的下注项目
//            txns.setBetType(cmdCallbackBetReq.getGame().toString());
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
//            txns.setWinningAmount(winloseAmount);
            //创建时间
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            //玩家下注时间
            txns.setBetTime(dateStr);
            //真实下注金额,需增加在玩家的金额
            txns.setRealBetAmount(betAmount);
            //真实返还金额,游戏赢分
//            txns.setRealWinAmount(winloseAmount);
            //此交易是否是投注 true是投注 false 否
            txns.setBet(false);
            //返还金额 (包含下注金额)
//            txns.setWinAmount(winloseAmount);
            //有效投注金额 或 投注面值
            txns.setTurnover(betAmount);
            //辨认交易时间依据
            txns.setTxTime(dateStr);
            //操作名称
            txns.setMethod("Settle");
            txns.setStatus("Running");
            //余额
            txns.setBalance(balance);
            txns.setCreateTime(dateStr);
            //投注 IP
            txns.setBetIp(ip);//  string 是 投注 IP
            txnsMapper.insert(txns);

            JSONObject jsonObject = initSuccessResponse();
            jsonObject.put("trx_id", reqId);
            jsonObject.put("balance", balance);
            jsonObject.put("BonusUsed", BigDecimal.ZERO);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(0, e.getMessage());
        }
    }

    @Override
    public Object creditCustomer(BtiCreditRequest btiCreditRequest, String ip, String reqId) {
        logger.info("bti_debitCustomer btiGame paramJson:{}, ip:{}", JSONObject.toJSONString(btiCreditRequest), ip);

        try {

            GameParentPlatform gameParentPlatform = getGameParentPlatform();
            // 校验IP
            if (checkIp(ip, gameParentPlatform)) {
                return initFailureResponse(0, "非信任來源IP");
            }

            String userName = btiCreditRequest.getCustomerID();

            // 查询订单
            MemTradingBO memBaseinfo = gameCommonService.getMemTradingInfo(userName);
            if (null == memBaseinfo) {
                return initFailureResponse(0, "用户不存在");
            }

            Txns oldTxns = getTxns(gameParentPlatform, reqId, null);
            // 重复订单
            if (null != oldTxns) {
                return initFailureResponse(0, "注单已结算");
            }

            // 所有中奖金额
            BigDecimal winAmount = btiCreditRequest.getAmount();

            GamePlatform gamePlatform = gameCommonService.getGamePlatformByParentName(OpenAPIProperties.BTI_PLATFORM_CODE).get(0);
            GameCategory gameCategory = gameCommonService.getGameCategoryById(gamePlatform.getCategoryId());


            BigDecimal balance = memBaseinfo.getBalance().add(winAmount);
            // 更新余额
            gameCommonService.updateUserBalance(memBaseinfo, winAmount, GoldchangeEnum.BETNSETTLE, TradingEnum.INCOME);

            Txns txns = new Txns();
            //游戏商注单号
            txns.setPlatformTxId(reqId);

            //玩家 ID
            txns.setUserId(memBaseinfo.getId().toString());
            //玩家货币代码
            txns.setCurrency(gameParentPlatform.getCurrencyType());
//            txns.setOdds(kaCallbackPlayReq.getBetPerSelection());
//            txns.setRoundId(cmdCallbackBetReq.getRound());
//            txns.setGameInfo(item.getBtiSelectionsRequest().getSelectionList().stream()
//                    .map(BtiCreditSelectionRequest::getLineID).collect(Collectors.joining(",")));
            //平台代码
            txns.setPlatform(gameParentPlatform.getPlatformCode());
            //平台名称
            txns.setPlatformEnName(gameParentPlatform.getPlatformEnName());
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
            txns.setBetAmount(BigDecimal.ZERO);
            //游戏平台的下注项目
//            txns.setBetType(cmdCallbackBetReq.getGame().toString());
            //中奖金额（赢为正数，亏为负数，和为0）或者总输赢
            //创建时间
            String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
            txns.setWinningAmount(winAmount);
            //玩家下注时间
            txns.setBetTime(dateStr);
            //真实下注金额,需增加在玩家的金额
            txns.setRealBetAmount(BigDecimal.ZERO);
            //真实返还金额,游戏赢分
            txns.setRealWinAmount(winAmount);
            //此交易是否是投注 true是投注 false 否
            txns.setBet(false);
            //返还金额 (包含下注金额)
            txns.setWinAmount(winAmount);
            //有效投注金额 或 投注面值
            txns.setTurnover(BigDecimal.ZERO);
            //辨认交易时间依据
            txns.setTxTime(dateStr);
            //操作名称
            txns.setMethod("Settle");
            txns.setStatus("Running");
            //余额
            txns.setBalance(balance);
            txns.setCreateTime(dateStr);
            //投注 IP
            txns.setBetIp(ip);//  string 是 投注 IP
            txnsMapper.insert(txns);


            JSONObject jsonObject = initSuccessResponse();
            jsonObject.put("trx_id", reqId);
            jsonObject.put("balance", balance);
            jsonObject.put("BonusUsed", BigDecimal.ZERO);
            return jsonObject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return initFailureResponse(0, e.getMessage());
        }
    }

    /**
     * 查询第三方订单是否存在
     *
     * @param gameParentPlatform gameParentPlatform
     * @param paySerialno        paySerialno
     * @param rePlatformTxId     req_id
     * @return Txns
     */
    private Txns getTxns(GameParentPlatform gameParentPlatform, String paySerialno, String rePlatformTxId) {
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet")
                .or().eq(Txns::getMethod, "Cancel Bet")
                .or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, paySerialno);
        wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
        if (StringUtils.isNotEmpty(rePlatformTxId)) {
            wrapper.eq(Txns::getRePlatformTxId, rePlatformTxId);
        }
        return txnsMapper.selectOne(wrapper);
    }

    /**
     * 查询预扣款注单, 不查询debit注单
     *
     * @param gameParentPlatform gameParentPlatform
     * @param paySerialno        paySerialno
     * @return Txns
     */
    private Txns getReserveTxns(GameParentPlatform gameParentPlatform, String paySerialno) {
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet")
                .or().eq(Txns::getMethod, "Cancel Bet")
                .or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, paySerialno);
        wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
        wrapper.isNull(Txns::getRePlatformTxId);
        return txnsMapper.selectOne(wrapper);
    }

    /**
     * 查询预扣款注单下的debit 注单
     *
     * @param gameParentPlatform gameParentPlatform
     * @param paySerialno        paySerialno
     * @return Txns
     */
    private List<Txns> getTxnsHasDebit(GameParentPlatform gameParentPlatform, String paySerialno) {
        LambdaQueryWrapper<Txns> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(c -> c.eq(Txns::getMethod, "Place Bet")
                .or().eq(Txns::getMethod, "Cancel Bet")
                .or().eq(Txns::getMethod, "Adjust Bet"));
        wrapper.eq(Txns::getStatus, "Running");
        wrapper.eq(Txns::getPlatformTxId, paySerialno);
        wrapper.eq(Txns::getPlatform, gameParentPlatform.getPlatformCode());
        wrapper.isNotNull(Txns::getRePlatformTxId);
        return txnsMapper.selectList(wrapper);
    }

    /**
     * 更新预扣注单状态
     *
     * @param platformTxId 预扣注单reserve ID
     * @param platformCode 游戏code
     * @param status       待修改状态
     */
    private void update(String platformTxId, String platformCode, String status) {
        LambdaUpdateWrapper<Txns> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Txns::getPlatformTxId, platformTxId);
        updateWrapper.eq(Txns::getPlatform, platformCode);

        Txns txns = new Txns();
        txns.setStatus(status);
        String dateStr = DateUtils.format(new Date(), DateUtils.newFormat);
        txns.setUpdateTime(dateStr);
        txnsMapper.update(txns, updateWrapper);
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
    private JSONObject initSuccessResponse() {
//        resp.put("responseDate", DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error_code", 0);
//        jsonObject.put("result", resp);
        return jsonObject;
    }

    /**
     * 初始化交互失败返回
     *
     * @param error       错误码
     * @param description 错误描述
     * @return JSONObject
     */
    private JSONObject initFailureResponse(Integer error, String description) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error_code", error);
        jsonObject.put("error_message", description);
        return jsonObject;
    }

    private GameParentPlatform getGameParentPlatform() {
        return gameCommonService.getGameParentPlatformByplatformCode(OpenAPIProperties.BTI_PLATFORM_CODE);
    }
}