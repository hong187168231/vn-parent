package com.indo.admin.modules.stat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.indo.admin.pojo.entity.StatAgentReport;
import com.indo.admin.modules.stat.req.AgentReportReq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author xxx
 * @since 2022-01-11
 */
@Mapper
public interface StatAgentReportMapper extends BaseMapper<StatAgentReport> {

    List<StatAgentReport> queryList(@Param("page") Page<StatAgentReport> page, @Param("req") AgentReportReq req);
}
