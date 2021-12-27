package com.indo.admin.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.indo.common.pojo.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 活动记录表
 * </p>
 *
 * @author puff
 * @since 2021-12-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "activity对象", description = "活动记录表")
@TableName("act_activity")
public class Activity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键id")
    @TableId(value = "act_id", type = IdType.AUTO)
    private Long actId;

    @ApiModelProperty(value = "活动类型id")
    private Long actTypeId;

    @ApiModelProperty(value = "活动图片地址")
    private String actImageUrl;

    @ApiModelProperty(value = "状态 0 下架 1 上架  2 已过期")
    private Integer status;

    @ApiModelProperty(value = "活动详情")
    private String content;

    @ApiModelProperty(value = "是否永久活动 0否 1 是")
    private Integer isPer;

    @ApiModelProperty(value = "活动开始时间")
    private LocalDateTime beginTime;

    @ApiModelProperty(value = "活动结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "更新人")
    private String updateUser;

    @ApiModelProperty(value = "备注")
    private String remark;


}