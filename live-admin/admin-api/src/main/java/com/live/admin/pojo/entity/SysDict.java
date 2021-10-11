package com.live.admin.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.live.common.pojo.entity.BaseEntity;
import lombok.Data;

@Data
public class

SysDict extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Integer status;

    private  String  remark;

}
