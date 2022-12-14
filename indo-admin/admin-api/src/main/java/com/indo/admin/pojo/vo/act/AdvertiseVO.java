package com.indo.admin.pojo.vo.act;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AdvertiseVO {

    @ApiModelProperty(value = "主键id")
    private Long adeId;

    @ApiModelProperty(value = "图片地址")
    private String imageUrl;

    @ApiModelProperty(value = "跳转地址")
    private String skipUrl;

    @ApiModelProperty(value = "内容详情")
    private String content;

    @ApiModelProperty(value = "状态 0 下架1 上架")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private String createTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "广告类型：1顶部广告，2底部广告，3推广广告，4banner广告")
    private Integer types;
}
