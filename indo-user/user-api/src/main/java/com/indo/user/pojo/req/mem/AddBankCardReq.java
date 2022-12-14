package com.indo.user.pojo.req.mem;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "添加请求参数类")
public class AddBankCardReq {

    @ApiModelProperty(value = "银行卡用户名", required = true)
    private String userName;

    @ApiModelProperty(value = "银行id", required = true)
    private Long bankId;

    @ApiModelProperty(value = "银行名称", required = true)
    private String bankName;

    @ApiModelProperty(value = "银行分行", required = true)
    private String bankBranch;

    @ApiModelProperty(value = "卡号", required = true)
    private String bankCardNo;

    @ApiModelProperty(value = "ifsc", required = true)
    private String ifsc;

    @ApiModelProperty(value = "城市", required = true)
    private String city;

    @ApiModelProperty(value = "电话", required = true)
    private String phone;

    @ApiModelProperty(value = "邮箱", required = true)
    private String email;
}
