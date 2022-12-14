package com.indo.user.pojo.req.msg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class StationLetterAddReq {


    @ApiModelProperty(value = "收件人集合")
    private List<String> receiver;

    @ApiModelProperty(value = "标题")
    @NotNull(message = "标题不能为空")
    private String title;

    @ApiModelProperty(value = "内容")
    @NotNull(message = "内容不能为空")
    private String content;

    @ApiModelProperty(value = "发送类型: 1 按收件人发送 2 按会员等级发送 3 按支付层级发送")
    @NotNull(message = "发送类型不能为空")
    private Integer sendType;

}
