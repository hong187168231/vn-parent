package com.indo.game.service.app.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.indo.common.constant.RedisConstants;
import com.indo.common.redis.utils.RedisUtils;
import com.indo.common.result.Result;
import com.indo.common.utils.CollectionUtil;
import com.indo.core.pojo.entity.Activity;
import com.indo.core.pojo.entity.PayWithdrawConfig;
import com.indo.game.mapper.TxnsMapper;
import com.indo.game.mapper.frontend.GameCategoryMapper;
import com.indo.game.mapper.frontend.GameParentPlatformMapper;
import com.indo.game.mapper.frontend.GamePlatformMapper;
import com.indo.game.pojo.dto.manage.GameInfoPageReq;
import com.indo.game.pojo.entity.manage.*;
import com.indo.game.pojo.vo.app.GameInfoAgentRecord;
import com.indo.game.pojo.vo.app.GameInfoRecord;
import com.indo.game.pojo.vo.app.GamePlatformRecord;
import com.indo.game.pojo.vo.app.GameStatiRecord;
import com.indo.game.service.app.IGameManageService;
import com.indo.game.service.common.GameCommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameManageServiceImpl implements IGameManageService {

    @Autowired
    private GamePlatformMapper gamePlatformMapper;
    @Autowired
    private GameParentPlatformMapper gameParentPlatformMapper;
    @Autowired
    private GameCategoryMapper gameCategoryMapper;
    @Autowired
    private TxnsMapper txnsMapper;
    @Autowired
    private GameCommonService gameCommonService;
    @Override
    public List<GameCategory> queryAllGameCategory() {
        List<GameCategory> categoryList;
        Map<Object, Object> map = RedisUtils.hmget(RedisConstants.GAME_CATEGORY_KEY);
        categoryList = new ArrayList(map.values());
        if (CollectionUtil.isEmpty(categoryList)) {
            LambdaQueryWrapper<GameCategory> wrapper = new LambdaQueryWrapper<>();
            categoryList = gameCategoryMapper.selectList(wrapper);
        }
        categoryList.sort(Comparator.comparing(GameCategory::getSortNumber));
        return categoryList;
    }

    @Override
    public List<GamePlatformRecord> queryAllGamePlatform() {
        List<GamePlatformRecord> platformList = gamePlatformMapper.queryAllGamePlatform();
        return platformList;
    }

    @Override
    public List<GamePlatformRecord> queryHotGamePlatform() {
        List<GamePlatformRecord> platformList = gamePlatformMapper.queryAllGamePlatform();
        if (CollectionUtil.isNotEmpty(platformList)) {
            platformList = platformList.stream()
                    .filter(platform -> platform.getIsHotShow().equals("1"))
                    .collect(Collectors.toList());
        }
        return platformList;
    }
    @Override
    public List<GamePlatformRecord> queryGamePlatformByCategory(Long categoryId) {
        List<GamePlatformRecord> platformList = queryAllGamePlatform();
        if (CollectionUtil.isNotEmpty(platformList)) {
            platformList = platformList.stream()
                    .filter(platform -> platform.getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }
        return platformList;
    }

    @Override
    public IPage<GameStatiRecord> queryAllGameInfoCount(GameInfoPageReq req) {
        IPage<GameStatiRecord> page = new Page<>(req.getPage(), req.getLimit());
        page.setRecords(txnsMapper.queryAllGameInfoCount(page, req));
        return page;
    }

    @Override
    public IPage<GameInfoRecord> queryAllGameInfo(GameInfoPageReq req) {
        IPage<GameInfoRecord> page = new Page<>(req.getPage(), req.getLimit());
        page.setRecords(txnsMapper.queryAllGameInfo(page, req));
        return page;
    }

    @Override
    public IPage<GameInfoAgentRecord> queryAllAgentGameInfo(GameInfoPageReq req) {
        IPage<GameInfoAgentRecord> page = new Page<>(req.getPage(), req.getLimit());
        page.setRecords(txnsMapper.queryAllAgentGameInfo(page, req));
        return page;
    }

    @Override
    public List<GameParentPlatform> queryAllGameParentPlatform() {
        List<GameParentPlatform> parentPlatformList;
        Map<Object, Object> map = RedisUtils.hmget(RedisConstants.GAME_PARENT_PLATFORM_KEY);
        parentPlatformList = new ArrayList(map.values());
        if (CollectionUtil.isEmpty(parentPlatformList)) {
            LambdaQueryWrapper<GameParentPlatform> wrapper = new LambdaQueryWrapper<>();
            parentPlatformList = gameParentPlatformMapper.selectList(wrapper);
        }
        if (CollectionUtil.isNotEmpty(parentPlatformList)) {
            parentPlatformList = parentPlatformList.stream()
                    .filter(parentPlatform -> parentPlatform.getIsStart().equals("1"))
                    .collect(Collectors.toList());
        }
        if (CollectionUtil.isNotEmpty(parentPlatformList)) {
            parentPlatformList = parentPlatformList.stream()
                    .filter(parentPlatform -> parentPlatform.getIsVirtual().equals("0"))
                    .collect(Collectors.toList());
        }
        parentPlatformList.sort(Comparator.comparing(GameParentPlatform::getSortNumber));
        return parentPlatformList;
    }

    @Override
    public List<GameParentPlatform> queryHotGameParentPlatform() {
        List<GameParentPlatform> platformList = this.queryAllGameParentPlatform();
        if (CollectionUtil.isNotEmpty(platformList)) {
            platformList = platformList.stream()
                    .filter(platform -> platform.getIsHotShow().equals("1"))
                    .collect(Collectors.toList());
        }
        return platformList;
    }
}
