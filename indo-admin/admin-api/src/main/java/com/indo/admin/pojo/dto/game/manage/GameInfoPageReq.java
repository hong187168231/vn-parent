package com.indo.admin.pojo.dto.game.manage;

import com.indo.common.base.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class GameInfoPageReq extends BaseDTO {

    @ApiModelProperty(value = "游戏平台代码")
    private List platform;

    @ApiModelProperty(value = "游戏分类ID")
    private List categoryId;

    @ApiModelProperty(value = "开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间")
    private String endTime;

    @ApiModelProperty(value = "排序方式，Asc：true，Desc：false")
    private Boolean orderBy;

    @ApiModelProperty(value = "用户账号")
    private String userAcct;

    @ApiModelProperty(value = "游戏名称")
    private String gameName;

    @ApiModelProperty(value = "游戏商注单号")
    private String platformTxId;


}
