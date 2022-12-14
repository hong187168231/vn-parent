package com.indo.admin.modules.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.indo.admin.pojo.dto.game.manage.GamePlatformPageReq;
import com.indo.core.pojo.entity.game.GamePlatform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminGamePlatformMapper extends BaseMapper<GamePlatform> {

    List<GamePlatform> queryAllGamePlatform(@Param("page") IPage<GamePlatform> page, @Param("req") GamePlatformPageReq req);
}
