package com.indo.game.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indo.common.constant.RedisKeys;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.result.Result;
import com.indo.common.web.exception.BizException;
import com.indo.core.pojo.dto.MemGoldChangeDto;
import com.indo.core.service.IMemGoldChangeService;
import com.indo.game.common.util.GameBusinessRedisUtils;
import com.indo.game.mapper.frontend.GameCategoryMapper;
import com.indo.game.mapper.frontend.GamePlatformMapper;
import com.indo.game.pojo.entity.manage.GameCategory;
import com.indo.game.pojo.entity.manage.GamePlatform;
import com.indo.game.service.common.GameCommonService;
import com.indo.user.api.MemBaseInfoFeignClient;
import com.indo.user.pojo.entity.MemBaseinfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service(value = "gameCommonService")
public class GameCommonServiceImpl implements GameCommonService {
    @Autowired
    private GamePlatformMapper gamePlatformMapper;

    @Autowired
    private GameCategoryMapper gameCategoryMapper;

    @Resource
    private MemBaseInfoFeignClient memBaseInfoFeignClient;
    @Resource
    private IMemGoldChangeService iMemGoldChangeService;

    @Override
    public GamePlatform getGamePlatformByplatformCode(String platformCode) {
        GamePlatform gamePlatform = GameBusinessRedisUtils.get(RedisKeys.GAME_PLATFORM_KEY + platformCode);
        if (null == gamePlatform) {
            LambdaQueryWrapper<GamePlatform> wrapper = new LambdaQueryWrapper<GamePlatform>();
            wrapper.eq(GamePlatform::getPlatformCode, platformCode);
            gamePlatform = gamePlatformMapper.selectOne(wrapper);
        }
        return gamePlatform;
    }

    @Override
    public GameCategory getGameCategoryById(Long id) {
        GameCategory gameCategory = GameBusinessRedisUtils.get(RedisKeys.GAME_PLATFORM_KEY + id);
        if (null == gameCategory) {
            LambdaQueryWrapper<GameCategory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GameCategory::getId, id);
            gameCategory = gameCategoryMapper.selectById(wrapper);
        }
        return gameCategory;
    }


    public MemBaseinfo getMemBaseInfo(String userId) {
        Result<MemBaseinfo> result = memBaseInfoFeignClient.getMemBaseInfo(Long.parseLong(userId));
        if (Result.success().getCode().equals(result.getCode())) {
            MemBaseinfo memBaseinfo = result.getData();
            return memBaseinfo;
        } else {
            throw new BizException("No client with requested id: " + userId);
        }
    }

    @Override
    public MemBaseinfo getByAccountNo(String accountNo) {
        Result<MemBaseinfo> result = memBaseInfoFeignClient.getByAccount(accountNo);
        MemBaseinfo memBaseinfo = null;
        if (null != result && Result.success().getCode().equals(result.getCode())) {
            memBaseinfo = result.getData();

        } else {
            throw new BizException("No client with requested accountNo: " + accountNo);
        }
        return memBaseinfo;
    }

    @Override
    public void updateUserBalance(MemBaseinfo memBaseinfo, BigDecimal changeAmount, GoldchangeEnum goldchangeEnum, TradingEnum tradingEnum) {
        MemGoldChangeDto goldChangeDO = new MemGoldChangeDto();
        goldChangeDO.setChangeAmount(changeAmount);
        goldChangeDO.setTradingEnum(tradingEnum);
        goldChangeDO.setGoldchangeEnum(goldchangeEnum);
        goldChangeDO.setUserId(memBaseinfo.getId());
        goldChangeDO.setUpdateUser(memBaseinfo.getAccount());
//      goldChangeDO.setRefId(rechargeOrder.getRechargeOrderId());
        boolean flag = iMemGoldChangeService.updateMemGoldChange(goldChangeDO);
        if (!flag) {
            throw new BizException("修改余额失败");
        }
    }


}
