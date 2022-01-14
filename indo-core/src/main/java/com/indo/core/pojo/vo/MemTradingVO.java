package com.indo.core.pojo.vo;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: kevin
 * @Date: 2021/8/30 16:05
 * @Version: 1.0.0
 * @Desc: 相应参数实体
 */
@Data
@ApiModel
public class MemTradingVO {

    @ApiModelProperty(value = "用户ID")
    private Long id;

    @ApiModelProperty(value = "余额")
    private Integer memLevel;

    @ApiModelProperty(value = "余额")
    private BigDecimal balance;

    @ApiModelProperty(value = "可提金额")
    private BigDecimal canAmount;

    @ApiModelProperty(value = "存款总额")
    private BigDecimal totalDeposit;

    @ApiModelProperty(value = "取款总额")
    private BigDecimal totalEnchashment;

    @ApiModelProperty(value = "总投注")
    private BigDecimal totalBet;

}
