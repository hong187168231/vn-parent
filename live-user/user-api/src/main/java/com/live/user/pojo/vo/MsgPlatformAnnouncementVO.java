package com.live.user.pojo.vo;

import com.live.user.pojo.entity.MsgPlatformAnnouncement;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class MsgPlatformAnnouncementVO extends MsgPlatformAnnouncement {

    @ApiModelProperty(value = "等级")
    private List<String> levelNames;
}
