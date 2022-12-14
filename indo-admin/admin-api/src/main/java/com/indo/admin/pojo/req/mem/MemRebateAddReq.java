package com.indo.admin.pojo.req.mem;

import com.indo.core.pojo.vo.mem.MemBetVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class MemRebateAddReq {


    @ApiModelProperty(value = "返点集合")
    private List<MemBetVo> betList;

}
