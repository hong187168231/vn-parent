package com.indo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.indo.admin.pojo.vo.agent.AgentSubVO;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.web.exception.BizException;
import com.indo.core.base.service.impl.SuperServiceImpl;
import com.indo.core.pojo.entity.AgentApply;
import com.indo.core.pojo.entity.AgentRelation;
import com.indo.user.mapper.AgentApplyMapper;
import com.indo.user.mapper.AgentRelationMapper;
import com.indo.user.pojo.req.mem.MemAgentApplyReq;
import com.indo.user.pojo.req.mem.SubordinateAppReq;
import com.indo.user.service.IMemAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 会员下级表 服务实现类
 * </p>
 *
 * @author puff
 * @since 2021-12-11
 */
@Service
public class MemAgentServiceImpl extends SuperServiceImpl<AgentRelationMapper, AgentRelation> implements IMemAgentService {


    @Autowired
    private AgentApplyMapper agentApplyMapper;


    @Autowired
    private AgentRelationMapper agentRelationMapper;


    @Override
    public boolean apply(MemAgentApplyReq req, LoginInfo loginInfo) {
        AgentApply agentApply = new AgentApply();
        agentApply.setMemId(loginInfo.getId());
        agentApply.setStatus(0);
        return agentApplyMapper.insert(agentApply) > 0;
    }

    @Override
    public Page<AgentSubVO> subordinatePage(SubordinateAppReq req, LoginInfo loginInfo) {
        LambdaQueryWrapper<AgentRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentRelation::getSuperior, loginInfo.getAccount());
        Integer agentCount = agentRelationMapper.selectCount(wrapper);
        if (agentCount < 1) {
            throw new BizException("你还没有下级");
        }
        wrapper.eq(AgentRelation::getAccount, req.getAccount());
        agentCount = agentRelationMapper.selectCount(wrapper);
        if (agentCount < 1) {
            throw new BizException("请输入正确的下级账号");
        }
        Page<AgentSubVO> page = new Page<>(req.getPage(), req.getLimit());
        List<AgentSubVO> agentList = agentRelationMapper.subordinateList(page, req);
        page.setRecords(agentList);
        return page;
    }
}
