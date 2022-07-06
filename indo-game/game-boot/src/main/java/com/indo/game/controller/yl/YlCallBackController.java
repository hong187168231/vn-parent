package com.indo.game.controller.yl;

import com.alibaba.fastjson.JSONObject;
import com.indo.common.annotation.AllowAccess;
import com.indo.common.utils.IPAddressUtil;
import com.indo.game.pojo.dto.ps.PsCallBackParentReq;
import com.indo.game.pojo.dto.yl.YlCallBackReq;
import com.indo.game.service.ps.PsCallbackService;
import com.indo.game.service.yl.YlCallbackService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/YL/callBack")
public class YlCallBackController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private YlCallbackService ylCallbackService;




    /**
     * 下注及结算
     */
    @RequestMapping(value = "/settleFishBet", method = RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @ResponseBody
    @AllowAccess
    public Object bet(@RequestBody JSONObject jsonObject, HttpServletRequest request) {

        String ip = IPAddressUtil.getIpAddress(request);
        logger.info("ylCallBack {} ylSettleFishBet回调,params:{}", JSONObject.toJSONString(jsonObject));
        YlCallBackReq ylCallBackReq =  JSONObject.toJavaObject(jsonObject,YlCallBackReq.class);
        Object object = ylCallbackService.psBetCallback(ylCallBackReq, ip);
        logger.info("ylCallBack {} ylSettleFishBet回调返回数据 params:{}", object);
        return object;
    }



    /**
     * 返还押注
     */
    @RequestMapping(value = "/voidFishBet", method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    @AllowAccess
    public Object voidFishBet(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        String ip = IPAddressUtil.getIpAddress(request);
        logger.info("ylCallBack {} voidFishBet回调,params:{}", JSONObject.toJSONString(jsonObject));
        YlCallBackReq ylCallBackReq  =  JSONObject.toJavaObject(jsonObject,YlCallBackReq.class);
        Object object = ylCallbackService.ylVoidFishBetCallback(ylCallBackReq, ip);
        logger.info("ylCallBack {} voidFishBet回调返回数据 params:{}", object);
        return object;
    }



    /**
     * 获取余额
     */
    @RequestMapping(value = "/GetBalance", method = RequestMethod.POST,produces = "application/json;charset=UTF-8")
    @ResponseBody
    @AllowAccess
    public Object getBalance(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        String ip = IPAddressUtil.getIpAddress(request);
        logger.info("ylCallBack {} ylGetBalance回调,params:{}", JSONObject.toJSONString(jsonObject));
        YlCallBackReq ylCallBackReq = JSONObject.toJavaObject(jsonObject,YlCallBackReq.class);
        Object object = ylCallbackService.ylGetBalanceCallback(ylCallBackReq, ip);
        logger.info("ylCallBack {} ylGetBalance回调返回数据 params:{}", object);
        return object;
    }
}
