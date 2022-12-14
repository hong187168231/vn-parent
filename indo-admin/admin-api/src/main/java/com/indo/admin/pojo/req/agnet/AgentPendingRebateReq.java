package com.indo.admin.pojo.req.agnet;

import com.indo.common.base.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class AgentPendingRebateReq extends BaseDTO {

    @ApiModelProperty("会员ID")
    private Long memId;

    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;
}
