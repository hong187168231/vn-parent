package com.indo.admin.pojo.req.mem;

import com.indo.common.base.BaseDTO;
import com.indo.common.pojo.param.QueryParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @des:会员基础信息查询参数
 * @Author: kevin
 */

@Data
@ApiModel
public class MemBaseInfoReq extends BaseDTO {

    @ApiModelProperty("会员ID")
    private Long id;
    @ApiModelProperty("会员账号")
    private String account;
    @ApiModelProperty("用户等级")
    private Integer memLevel;
    @ApiModelProperty("层级ID")
    private Long groupId;
    @ApiModelProperty("真实姓名")
    private String realName;
    @ApiModelProperty("注册邀请码")
    private String inviteCode;
    @ApiModelProperty("账户状态:0-正常,1-删除,2-冻结")
    private Integer status;
    @ApiModelProperty("注册开始时间")
    private Date startTime;
    @ApiModelProperty("注册结束时间")
    private Date endTime;
    @ApiModelProperty(value = "ip地址")
    private String clientIp;
}
