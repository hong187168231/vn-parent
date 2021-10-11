package com.live.pay.pojo.dto;

import com.live.pay.pojo.entity.PayGroupConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 支付层级配置请求参数
 */
@Data
public class PayGroupConfigDto extends PayGroupConfig {
    private static final long serialVersionUID = 1L;


    @ApiModelProperty(value = "当前页")
    private Integer page;

    @ApiModelProperty(value = "分页数")
    private Integer limit;
}
