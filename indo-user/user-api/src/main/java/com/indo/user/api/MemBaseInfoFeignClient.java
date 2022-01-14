package com.indo.user.api;


import com.indo.common.constant.ServiceIdConstant;
import com.indo.common.result.Result;
import com.indo.common.web.exception.KeepErrMsgConfiguration;
import com.indo.user.api.fallback.MemBaseInfoFeignFallback;
import com.indo.user.pojo.bo.MemTradingBO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = ServiceIdConstant.USER_SERVICE_ID, contextId = "oauth-client",
        fallbackFactory = MemBaseInfoFeignFallback.class, configuration = {KeepErrMsgConfiguration.class})
public interface MemBaseInfoFeignClient {


    @GetMapping("/rpc/memBaseInfo/getMemTradingInfo/{account}")
    Result<MemTradingBO> getMemTradingInfo(@PathVariable String account);




}
