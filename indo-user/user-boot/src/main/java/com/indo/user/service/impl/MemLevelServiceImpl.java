package com.indo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.indo.common.enums.GiftEnum;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.core.mapper.MemLevelMapper;
import com.indo.core.pojo.entity.MemLevel;
import com.indo.core.pojo.bo.MemTradingBO;
import com.indo.user.pojo.vo.level.Gift;
import com.indo.user.pojo.vo.level.LevelInfo;
import com.indo.user.pojo.vo.level.LevelUpRuleVO;
import com.indo.user.pojo.vo.level.MemLevelVo;
import com.indo.user.service.AppMemBaseInfoService;
import com.indo.user.service.IMemGiftReceiveService;
import com.indo.user.service.IMemLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * 会员等级表 服务实现类
 * </p>
 *
 * @author xxx
 * @since 2021-11-21
 */
@Service
public class MemLevelServiceImpl extends ServiceImpl<MemLevelMapper, MemLevel> implements IMemLevelService {

    @Autowired
    private AppMemBaseInfoService memBaseInfoService;

    @Autowired
    private IMemGiftReceiveService iMemGiftReceiveService;

    @Resource
    private MemLevelMapper memLevelMapper;

    @Override
    public MemLevelVo findInfo(LoginInfo loginInfo) {
        Long memId = loginInfo.getId();

        MemLevelVo memLevelVo = new MemLevelVo();
        MemTradingBO memTradingVo = memBaseInfoService.tradingInfo(loginInfo.getAccount());
        memLevelVo.setTradingVo(memTradingVo);

        MemLevel userLevel = memLevelMapper.selectById(memTradingVo.getMemLevel());

        List<LevelInfo> levelInfoList = new ArrayList<>();
        List<MemLevel> list = getLevelList();

        if (!CollectionUtils.isEmpty(list)) {
            for (MemLevel memLevel : list) {
                if (memLevel == null) {
                  continue;
                }
                List<Gift> giftList = new ArrayList<>();
                LevelInfo levelInfo = new LevelInfo();

                levelInfo.setLevel(memLevel.getLevel());
                levelInfo.setPromotionGift(memLevel.getReward());
                levelInfo.setNeedDeposit(memLevel.getNeedDeposit());
                levelInfo.setNeedBet(memLevel.getNeedBet());

                if (memLevel.getReward() != null) {
                    Gift gift = new Gift();
                    gift.setGiftEnum(GiftEnum.reward);
                    gift.setGiftName(GiftEnum.reward.getName());
                    gift.setAmount(memLevel.getReward().intValue());
                    if (userLevel.getLevel() >= memLevel.getLevel()) {
                        int count = iMemGiftReceiveService.countRewardReceive(memId, GiftEnum.reward.name(), memLevel.getLevel() + 1);
                        // 0 不可领取 1 可领取 2 已经领取
                        gift.setReceiveStatus(count == 0 ? 1 : 2);
                    }
                    giftList.add(gift);
                }
                if (memLevel.getEverydayGift() != null) {
                    Gift gift = new Gift();
                    gift.setGiftEnum(GiftEnum.everyday);
                    gift.setGiftName(GiftEnum.everyday.getName());
                    gift.setAmount(memLevel.getEverydayGift().intValue());
                    if (userLevel.getLevel().equals(memLevel.getLevel())) {
                        int count = iMemGiftReceiveService.countTodayReceive(memId, GiftEnum.everyday.name());
                        gift.setReceiveStatus(count == 0 ? 1 : 2);
                    }
                    giftList.add(gift);
                }
                if (memLevel.getWeekGift() != null) {
                    Gift gift = new Gift();
                    gift.setGiftEnum(GiftEnum.week);
                    gift.setGiftName(GiftEnum.week.getName());
                    gift.setAmount(memLevel.getWeekGift().intValue());
                    if (userLevel.getLevel().equals(memLevel.getLevel())) {
                        int count = iMemGiftReceiveService.countWeekReceive(memId, GiftEnum.week.name());
                        gift.setReceiveStatus(count == 0 ? 1 : 2);
                    }
                    giftList.add(gift);
                }
                if (memLevel.getMonthGift() != null) {
                    Gift gift = new Gift();
                    gift.setGiftEnum(GiftEnum.month);
                    gift.setGiftName(GiftEnum.month.getName());
                    gift.setAmount(memLevel.getMonthGift().intValue());
                    if (userLevel.getLevel().equals(memLevel.getLevel())) {
                        int count = iMemGiftReceiveService.countMonthReceive(memId, GiftEnum.month.name());
                        gift.setReceiveStatus(count == 0 ? 1 : 2);
                    }
                    giftList.add(gift);
                }
                if (memLevel.getYearGift() != null) {
                    Gift gift = new Gift();
                    gift.setGiftEnum(GiftEnum.year);
                    gift.setGiftName(GiftEnum.year.getName());
                    gift.setAmount(memLevel.getYearGift().intValue());
                    if (userLevel.getLevel().equals(memLevel.getLevel())) {
                        int count = iMemGiftReceiveService.countYearReceive(memId, GiftEnum.year.name());
                        gift.setReceiveStatus(count == 0 ? 1 : 2);
                    }
                    giftList.add(gift);
                }
                if (memLevel.getBirthdayGift() != null) {
                    Gift gift = new Gift();
                    gift.setGiftEnum(GiftEnum.birthday);
                    gift.setGiftName(GiftEnum.birthday.getName());
                    gift.setAmount(memLevel.getBirthdayGift().intValue());
                    if (userLevel.getLevel().equals(memLevel.getLevel())) {
                        int count = iMemGiftReceiveService.countBirthdayReceive(memId, GiftEnum.birthday.name());
                        gift.setReceiveStatus(count == 0 ? 1 : 2);
                    }
                    giftList.add(gift);
                }
                levelInfo.setGiftList(giftList);
                levelInfoList.add(levelInfo);
            }
        }

        memLevelVo.setLevelList(levelInfoList);
        if(memLevelVo.getTradingVo().getMemLevel()!=null||memLevelVo.getTradingVo().getMemLevel()>0){
           MemLevel memLevel = memLevelMapper.selectById(memLevelVo.getTradingVo().getMemLevel());
           if(memLevel!=null){
               memLevelVo.getTradingVo().setLevel(memLevel.getLevel());
           }
        }
        return memLevelVo;
    }

    @Override
    public List<LevelUpRuleVO> upRule(LoginInfo loginInfo) {
        List<LevelUpRuleVO> upRuleVOS = new LinkedList<>();
        List<MemLevel> levelList = getLevelList();
        for (MemLevel memLevel : levelList) {
            LevelUpRuleVO levelUpRuleVO = new LevelUpRuleVO();
            levelUpRuleVO.setNeedBet(memLevel.getNeedBet());
            levelUpRuleVO.setNeedDeposit(memLevel.getNeedDeposit());
            upRuleVOS.add(levelUpRuleVO);
        }
        return upRuleVOS;
    }


    private List<MemLevel> getLevelList() {
        LambdaQueryWrapper<MemLevel> wrapper = new LambdaQueryWrapper<>();
        List<MemLevel> list = this.baseMapper.selectList(wrapper);
        return list;
    }
}
