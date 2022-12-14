package com.indo.pay.pojo.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.indo.common.pojo.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 支付方式配置
 * </p>
 *
 * @author xxx
 * @since 2021-12-20
 */
@Data
public class PayWay {


    @ApiModelProperty(value = "支付方式id")
    private Long payWayId;

    @ApiModelProperty(value = "支付通道id")
    private Long payChannelId;

    @ApiModelProperty(value = "支付方式名称")
    private String wayName;

    private String bankName;

    @ApiModelProperty(value = "支付账号")
    private String bankAccount;

    @ApiModelProperty(value = "支付层id")
    private Long groupId;

    @ApiModelProperty(value = "审核状态 1 手动 2自动")
    private Integer auditStatus;

    @ApiModelProperty(value = "提示")
    private String tips;

    @ApiModelProperty(value = "最小金额")
    private Integer minAmount;

    @ApiModelProperty(value = "最大金额")
    private Integer maxAmount;

    @ApiModelProperty(value = "状态 0 正常 1停用")
    private Integer status;

    @ApiModelProperty(value = "排序")
    private Integer sortBy;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "更新人")
    private String updateUser;

    @ApiModelProperty(value = "备注")
    private String remark;


}
