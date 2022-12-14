package com.indo.core.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.indo.common.pojo.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * <p>
 * 会员下级表
 * </p>
 *
 * @author xxx
 * @since 2021-12-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_relation")
@ApiModel(value = "Agent对象", description = "会员下级表")
public class AgentRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "agent_id", type = IdType.AUTO)
    private Long agentId;

    @ApiModelProperty(value = "用户ID")
    private Long memId;

    @ApiModelProperty(value = "用户账号")
    private String account;

    @ApiModelProperty(value = "团队数")
    private Integer teamNum;

    @ApiModelProperty(value = "下级用户ID")
    private String subUserIds;

    @ApiModelProperty(value = "父ID")
    private Long parentId;

    @ApiModelProperty(value = "上级代理")
    private String superior;

    @ApiModelProperty(value = "状态 0=删除,1=正常")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;


}
