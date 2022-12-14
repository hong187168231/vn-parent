package com.indo.pay.service.payment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.indo.common.enums.CountryEnum;
import com.indo.common.enums.ThirdPayChannelEnum;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.redis.utils.GeneratorIdUtil;
import com.indo.common.result.Result;
import com.indo.common.web.exception.BizException;
import com.indo.pay.common.constant.PayConstants;
import com.indo.pay.factory.OnlinePaymentService;
import com.indo.pay.pojo.bo.PayChannel;
import com.indo.pay.pojo.bo.PayWay;
import com.indo.pay.pojo.bo.RechargeBO;
import com.indo.pay.pojo.dto.RechargeDTO;
import com.indo.pay.pojo.req.EasyPayReq;
import com.indo.pay.pojo.req.HuaRenPayReq;
import com.indo.pay.pojo.req.RechargeReq;
import com.indo.pay.pojo.req.SevenPayReq;
import com.indo.pay.pojo.resp.BasePayResp;
import com.indo.pay.pojo.resp.EasyPayResp;
import com.indo.pay.pojo.resp.HuaRenPayResp;
import com.indo.pay.pojo.resp.SevenPayResp;
import com.indo.pay.pojo.vo.PayWayVO;
import com.indo.pay.service.IPayWayService;
import com.indo.pay.service.IRechargeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author puff
 * @Description: 支付业务类
 * @date 2021/9/5 15:57
 */
@Component
@Slf4j
public class PaymentService {

    @Resource(name = "huaRenOnlinePaymentService")
    private OnlinePaymentService huaRenOnlinePaymentService;
    @Resource(name = "easyOnlinePaymentService")
    private OnlinePaymentService easyOnlinePaymentService;
    @Resource(name = "sevenOnlinePaymentService")
    private OnlinePaymentService sevenOnlinePaymentService;

    @Autowired
    private IRechargeService rechargeService;
    @Autowired
    private IPayWayService payWayService;

    private static final String DEFAULT_PAY_WAY = "h5";

    public Result paymentRequestByUser(LoginInfo loginInfo, RechargeReq rechargeReq, HttpServletRequest request) {
        Result result;
        // 业务逻辑校验
        RechargeBO rechargeBO = rechargeService.logicConditionCheck(rechargeReq, loginInfo);
        String countryCode = request.getHeader("countryCode");
        ThirdPayChannelEnum payChannel;
        if (CountryEnum.IN.getCode().equals(countryCode)) {
          payChannel = ThirdPayChannelEnum.HUAREN;
        } else {
          payChannel = ThirdPayChannelEnum.SEVEN;
        }
        // ThirdPayChannelEnum payChannel = ThirdPayChannelEnum.valueOf(rechargeBO.getPayChannel().getChannelCode());
        switch (payChannel) {
            case HUAREN:
                result = huarenPay(loginInfo, rechargeReq.getAmount(), rechargeBO.getPayChannel(), rechargeBO.getPayWay());
                break;
            case EASY:
                result = easyPay(loginInfo, rechargeReq.getAmount(), rechargeBO.getPayChannel(), rechargeBO.getPayWay());
                break;
            case SEVEN:
                result = sevenPay(loginInfo, rechargeReq, rechargeBO.getPayChannel(), rechargeBO.getPayWay());
                break;
            default:
                throw new BizException("请选择正确的支付方式");
        }
        return result;
    }


    public boolean insertPayment(RechargeDTO rechargeDTO) {
        return rechargeService.saveRechargeRecord(rechargeDTO);
    }


    /**
     * 创建支付成功返回url
     */
    private Result createPaySuccess(Integer code, String url) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("url", url);
        return Result.success(jsonObject);
    }


    /**
     * huaren支付
     */
    private Result huarenPay(LoginInfo loginInfo, BigDecimal amount, PayChannel payChannel, PayWay payWay) {
        HuaRenPayReq req = new HuaRenPayReq();
        try {
            log.info("paylog huaren支付 创建订单");
            req.setMemId(loginInfo.getId());
            req.setMerchantNo(payChannel.getMerchantNo());
            req.setNotifyUrl(payChannel.getNotifyUrl());
            req.setPageUrl(payChannel.getPageUrl());
            req.setPayType(payChannel.getChannelType() + "");
            req.setPayUrl(payChannel.getPayUrl());
            req.setSecretKey(payChannel.getSecretKey());
            req.setMerchantOrderNo(GeneratorIdUtil.generateId());
            req.setTradeAmount(amount);
            req.setPayChannelId(payChannel.getPayChannelId());
            req.setPayWayId(payWay.getPayWayId());
            // 支付请求
            BasePayResp huarenPayResp = huaRenOnlinePaymentService.onlinePayment(req, HuaRenPayResp.class);
            // 请求结果 flag为false表示异常
            if (!huarenPayResp.getFlag()) {
                return Result.failed("huaren支付失败：" + huarenPayResp.getMsg());
            }
            return createPaySuccess(PayConstants.PAY_RETURN_CODE_ZERO, huarenPayResp.getHtml());
        } catch (Exception e) {
            log.error("paylog huaren支付 创建订单失败 {} huarenPay {}", this.getClass().getName(), JSON.toJSONString(req), e);
        }
        return Result.failed("huaren支付失败");
    }


    /**
     * easy支付
     */
    private Result easyPay(LoginInfo loginInfo, BigDecimal amount, PayChannel payChannel, PayWay payWay) {
        EasyPayReq req = new EasyPayReq();
        try {
            log.info("paylog easyPay支付 创建订单");
            req.setMemId(loginInfo.getId());
            req.setMerchantNo(payChannel.getMerchantNo());
            req.setNotifyUrl(payChannel.getNotifyUrl());
            req.setPageUrl(payChannel.getPageUrl());
            req.setPayUrl(payChannel.getPayUrl());
            req.setSecretKey(payChannel.getSecretKey());
            req.setMerchantOrderNo(GeneratorIdUtil.generateId());
            req.setTradeAmount(amount);
            req.setPayChannelId(payChannel.getPayChannelId());
            req.setPayWayId(payWay.getPayWayId());
            // 支付请求
            EasyPayResp easyPayResp = easyOnlinePaymentService.onlinePayment(req, EasyPayResp.class);
            // 请求结果 flag为false表示异常
            if (!easyPayResp.getFlag()) {
                return Result.failed("easy支付失败：" + easyPayResp.getMsg());
            }
            return createPaySuccess(PayConstants.PAY_RETURN_CODE_ZERO, easyPayResp.getHtml());
        } catch (Exception e) {
            log.error("paylog easy支付 创建订单失败 {} easyPay {}", this.getClass().getName(), JSON.toJSONString(req), e);
        }
        return Result.failed("easy支付失败");
    }


    /**
     * 777支付
     */
    private Result sevenPay(LoginInfo loginInfo, RechargeReq rechargeReq, PayChannel payChannel, PayWay payWay) {
        SevenPayReq req = new SevenPayReq();
        try {
            log.info("paylog sevenPay支付 创建订单");
            req.setMemId(loginInfo.getId());
            req.setMerchantNo(payChannel.getMerchantNo());
            req.setNotifyUrl(payChannel.getNotifyUrl());
            req.setPageUrl(payChannel.getPageUrl());
            req.setPayUrl(payChannel.getPayUrl());
            req.setSecretKey(payChannel.getSecretKey());
            req.setMerchantOrderNo(GeneratorIdUtil.generateId());
            req.setPayChannelId(payChannel.getPayChannelId());
            req.setPayWayId(Objects.nonNull(payWay) && Objects.nonNull(payWay.getPayWayId()) ? payWay.getPayWayId() : getDefaultPayWayId(loginInfo, payChannel.getPayChannelId()));
            req.setTradeAmount(rechargeReq.getAmount().setScale(4));
            req.setType(rechargeReq.getPayBankCode());
            // 支付请求
            SevenPayResp sevenPayResp = sevenOnlinePaymentService.onlinePayment(req, SevenPayResp.class);
            // 请求结果 flag为false表示异常
            if (!sevenPayResp.getFlag()) {
                return Result.failed("777支付失败：" + sevenPayResp.getMsg());
            }
            return createPaySuccess(PayConstants.PAY_RETURN_CODE_ZERO, sevenPayResp.getHtml());
        } catch (Exception e) {
            log.error("paylog 777支付 创建订单失败 {} 777Pay {}", this.getClass().getName(), JSON.toJSONString(req), e);
        }
        return Result.failed("777支付失败");
    }

    /**
     * 默认H5支付方式
     * @param loginInfo
     * @param payChannelId
     * @return
     */
    private Long getDefaultPayWayId(LoginInfo loginInfo, Long payChannelId) {
        List<PayWayVO> payWayVoList = payWayService.wayList(loginInfo, payChannelId);
        if (CollectionUtils.isEmpty(payWayVoList)) {
            return null;
        }
        for (PayWayVO vo : payWayVoList) {
            if (DEFAULT_PAY_WAY.equals(vo.getWayName())) {
                return vo.getPayWayId();
            }
        }
        return null;
    }

}
