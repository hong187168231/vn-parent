package com.indo.admin.modules.test;

import com.indo.common.annotation.AllowAccess;
import com.indo.common.config.OpenAPIProperties;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.result.Result;
import com.indo.user.pojo.bo.MemTradingBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/test")
@Slf4j
@AllArgsConstructor
@Api(tags = "后台游戏管理接口")
public class TestController {


    @ApiOperation(value = "游戏记录", httpMethod = "GET")
    @GetMapping(value = "/hello")
    public Result queryGameRecord() {
        return Result.success("hello word");
    }


}
