package com.indo.admin.modules.mem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.indo.admin.pojo.dto.MemBetInfoDTO;
import com.indo.admin.pojo.req.mem.MemBaseInfoReq;
import com.indo.admin.pojo.vo.mem.MemBaseInfoVo;
import com.indo.admin.pojo.vo.mem.MemBetInfoVo;
import com.indo.core.pojo.bo.MemBaseInfoBO;
import com.indo.core.pojo.entity.MemBaseinfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 会员基础信息表 Mapper 接口
 * </p>
 *
 * @author kevin
 * @since 2021-10-23
 */
@Mapper
public interface MemBaseinfoMapper extends BaseMapper<MemBaseinfo> {

    List<MemBaseInfoVo> queryList(@Param("page") Page<MemBaseInfoVo> page,@Param("dto") MemBaseInfoReq dto);

    List<Long> findIdListByCreateTime(@Param("date") String date);

    MemBaseInfoBO findMemBaseInfoByAccount(@Param("account") String account);

    Page<MemBaseInfoBO> findIpRepeatAll(@Param("page") Page<MemBaseInfoBO> page,@Param("dto") MemBaseInfoReq dto);

    Page<MemBetInfoVo> findMemBetInfo (@Param("page") Page<MemBetInfoVo> page,@Param("dto") MemBetInfoDTO dto);
}
