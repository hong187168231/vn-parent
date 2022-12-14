package com.indo.pay.pojo.resp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
@Data
@ApiModel(value = "用户金额")
public class SafeboxMoneyResp implements Serializable {

    @ApiModelProperty(value = "保险箱金额")
    private BigDecimal userSafemoney;

    @ApiModelProperty(value = "用户余额")
    private BigDecimal userBalance;
}
