package com.indo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.indo.common.enums.GiftNameEnum;
import com.indo.common.enums.GiftTypeEnum;
import com.indo.common.enums.GoldchangeEnum;
import com.indo.common.enums.TradingEnum;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.common.utils.DateUtils;
import com.indo.common.web.exception.BizException;
import com.indo.core.pojo.dto.MemGoldChangeDTO;
import com.indo.core.pojo.entity.MemGiftReceive;
import com.indo.core.pojo.entity.MemLevel;
import com.indo.core.service.IMemGoldChangeService;
import com.indo.user.mapper.MemBaseInfoMapper;
import com.indo.user.mapper.MemGiftReceiveMapper;
import com.indo.user.pojo.req.gift.GiftReceiveReq;
import com.indo.user.service.IMemGiftReceiveService;
import com.indo.user.service.IMemLevelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 活动类型表 服务实现类
 * </p>
 *
 * @author puff
 * @since 2021-12-22
 */
@Service
public class MemGiftReceiveServiceImpl extends ServiceImpl<MemGiftReceiveMapper, MemGiftReceive> implements IMemGiftReceiveService {

    @Resource
    private IMemGoldChangeService iMemGoldChangeService;

    @Resource
    private MemGiftReceiveMapper memGiftReceiveMapper;

    @Resource
    private IMemLevelService memLevelService;
    @Resource
    private MemBaseInfoMapper memBaseInfoMapper;
    /**
     * 礼金领取
     *
     * @param req
     * @param loginInfo
     * @return
     */
    @Override
    @Transactional
    public boolean saveMemGiftReceive(GiftReceiveReq req, LoginInfo loginInfo) {
        // 根据会员等级id查询会员等级
        MemLevel memLevel = memLevelService.getById(loginInfo.getMemLevel());
        // 检查领取状态
        checkReceiveFlag(req, loginInfo);
        // 保存记录
        MemGiftReceive memGiftReceive = new MemGiftReceive();
        memGiftReceive.setMemId(loginInfo.getId());
        memGiftReceive.setGiftType(req.getGiftTypeEnum().getCode());
        memGiftReceive.setGiftCode(req.getGiftNameEnum().name());
        memGiftReceive.setGiftName(req.getGiftNameEnum().getName());
        memGiftReceive.setGiftAmount(req.getGiftAmount());
        if (req.getGiftNameEnum().equals(GiftNameEnum.reward)) {
            if (req.getLevel() <= 10) {
                memGiftReceive.setUpLevel(req.getLevel() + 1);
            } else {
                return false;
            }
        }
        // 插入礼金领取记录并更新账变信息
        if (this.baseMapper.insert(memGiftReceive) > 0) {
            updateGiftGold(req, loginInfo);
            return true;
        }
        return false;
    }

    @Override
    public MemGiftReceive findGiftByCodeAndMemId(String giftCode, Long memId) {
        LambdaQueryWrapper<MemGiftReceive> wrapper = new LambdaQueryWrapper();
        wrapper.eq(MemGiftReceive::getGiftCode, giftCode).eq(MemGiftReceive::getMemId, memId);
        return this.baseMapper.selectOne(wrapper);
    }

    /**
     * 检查礼金领取状态
     *
     * @param req
     * @param loginInfo
     */
    private void checkReceiveFlag(GiftReceiveReq req, LoginInfo loginInfo) {
        switch (req.getGiftTypeEnum()) {
            case vip:
                checkVipGift(req, loginInfo);
                break;
            default:
                throw new BizException("您不能领取该奖励");
        }

    }

    /**
     * 检查vip礼金领取状态
     *
     * @param req
     * @param loginInfo
     */
    private void checkVipGift(GiftReceiveReq req, LoginInfo loginInfo) {
        GiftNameEnum giftNameEnum = req.getGiftNameEnum();
        Long memId = loginInfo.getId();
        MemLevel memLevel = memLevelService.getById(loginInfo.getMemLevel());
        BigDecimal BetMoney = memBaseInfoMapper.findUserBetMoney(loginInfo.getAccount());
        if(BetMoney==null){
           BetMoney = BigDecimal.ZERO;
        }
        switch (giftNameEnum) {
            case reward:
                if (memLevel.getLevel() >= req.getLevel()) {
                    int count = countRewardReceive(memId, giftNameEnum.name(), req.getLevel() + 1);
                    if (count > 0) {
                        throw new BizException("您已领取过晋级奖励，请勿重复提交");
                    }
                    if(BetMoney.compareTo(memLevel.getNeedBet())<0){
                        throw new BizException("有效投注未达标");
                    }
                    req.setGiftAmount(memLevel.getReward());
                } else {
                    throw new BizException("您不能领取该奖励");
                }
                break;
            case everyday:
                if (memLevel.getLevel().equals(req.getLevel())) {
                    int countToday = countTodayReceive(memId, giftNameEnum.name());
                    if (countToday > 0) {
                        throw new BizException("您已领取过当日礼金，请勿重复提交");
                    }
                } else {
                    throw new BizException("您不能领取该奖励");
                }
                if(BetMoney.compareTo(memLevel.getNeedBet())<0){
                    throw new BizException("有效投注未达标");
                }
                req.setGiftAmount(memLevel.getEverydayGift());
                break;
            case week:
                if (memLevel.getLevel().equals(req.getLevel())) {
                    int countWeek = countWeekReceive(memId, giftNameEnum.name());
                    if (countWeek > 0) {
                        throw new BizException("您已领取过本周礼金，请勿重复提交");
                    }
                } else {
                    throw new BizException("您不能领取该奖励");
                }
                if(BetMoney.compareTo(memLevel.getNeedBet())<0){
                    throw new BizException("有效投注未达标");
                }
                req.setGiftAmount(memLevel.getWeekGift());
                break;
            case month:
                if (memLevel.getLevel().equals(req.getLevel())) {
                    int countMonth = countMonthReceive(memId, giftNameEnum.name());
                    if (countMonth > 0) {
                        throw new BizException("您已领取过本月礼金，请勿重复提交");
                    }
                } else {
                    throw new BizException("您不能领取该奖励");
                }
                if(BetMoney.compareTo(memLevel.getNeedBet())<0){
                    throw new BizException("有效投注未达标");
                }
                req.setGiftAmount(memLevel.getMonthGift());
                break;
            case year:
                if (memLevel.getLevel().equals(req.getLevel())) {
                    int countYear = countYearReceive(memId, giftNameEnum.name());
                    if (countYear > 0) {
                        throw new BizException("您已领取过本年礼金，请勿重复提交");
                    }
                } else {
                    throw new BizException("您不能领取该奖励");
                }
                if(BetMoney.compareTo(memLevel.getNeedBet())<0){
                    throw new BizException("有效投注未达标");
                }
                req.setGiftAmount(memLevel.getYearGift());
                break;
            case birthday:
                if (memLevel.getLevel().equals(req.getLevel())) {
                    int countBirthday = countBirthdayReceive(memId, giftNameEnum.name());
                    if (countBirthday > 0) {
                        throw new BizException("您已领取过生日礼金，请勿重复提交");
                    }
                } else {
                    throw new BizException("您不能领取该奖励");
                }
                if(BetMoney.compareTo(memLevel.getNeedBet())<0){
                    throw new BizException("有效投注未达标");
                }
                req.setGiftAmount(memLevel.getBirthdayGift());
                break;
        }

    }

    /**
     * 检查注册奖励领取状态
     *
     * @param loginInfo
     */
    private void checkRegisterGift(LoginInfo loginInfo) {
        int count = memGiftReceiveMapper.countRegisterGift(loginInfo.getId(), GiftTypeEnum.register.getCode(), GiftNameEnum.register.name());
        if (count > 0) {
            throw new BizException("您已领取过注册奖励，请勿重复提交");
        }
    }

    /**
     * 保存礼金领取账变记录
     *
     * @param giftReceiveReq
     * @param loginInfo
     */
    public void updateGiftGold(GiftReceiveReq giftReceiveReq, LoginInfo loginInfo) {
        MemGoldChangeDTO goldChangeDO = new MemGoldChangeDTO();
        goldChangeDO.setChangeAmount(giftReceiveReq.getGiftAmount());
        goldChangeDO.setTradingEnum(TradingEnum.INCOME);
        goldChangeDO.setGoldchangeEnum(GoldchangeEnum.valueOf(giftReceiveReq.getGiftNameEnum().name()));
        goldChangeDO.setUserId(loginInfo.getId());
        goldChangeDO.setUpdateUser(loginInfo.getAccount());
        Boolean flag = iMemGoldChangeService.updateMemGoldChange(goldChangeDO);
        if (!flag) {
            throw new BizException("领取礼金修改账变信息失败");
        }
    }

    /**
     * 统计晋级奖励
     *
     * @param memId
     * @param giftCode
     * @param upLevel
     * @return
     */
    public Integer countRewardReceive(Long memId, String giftCode, Integer upLevel) {
        int rewardCount = memGiftReceiveMapper.countVipUpLevelGift(memId, giftCode, upLevel);
        return rewardCount;
    }

    /**
     * 统计每日礼金
     *
     * @param memId
     * @param giftCode
     * @return
     */
    public Integer countTodayReceive(Long memId, String giftCode) {
        Date todayBeginTime = DateUtils.getDayBegin();
        Date todayEndTime = DateUtils.getDayEnd();
        int countToday = memGiftReceiveMapper.countVipTimeIntervalGift(memId, giftCode, todayBeginTime, todayEndTime);
        return countToday;
    }


    /**
     * 统计每周礼金
     *
     * @param memId
     * @param giftCode
     * @return
     */
    public Integer countWeekReceive(Long memId, String giftCode) {
        Date weekBeginTime = DateUtils.getBeginDayOfWeek();
        Date weekEndTime = DateUtils.getEndDayOfWeek();
        int countWeek = memGiftReceiveMapper.countVipTimeIntervalGift(memId, giftCode, weekBeginTime, weekEndTime);
        return countWeek;
    }

    /**
     * 统计每月礼金
     *
     * @param memId
     * @param giftCode
     * @return
     */
    public Integer countMonthReceive(Long memId, String giftCode) {
        Date monthBeginTime = DateUtils.getMonthBegin();
        Date monthEndTime = DateUtils.getMonthEnd();
        int countMonth = memGiftReceiveMapper.countVipTimeIntervalGift(memId, giftCode, monthBeginTime, monthEndTime);
        return countMonth;
    }

    /**
     * 统计每年礼金
     *
     * @param memId
     * @param giftCode
     * @return
     */
    public Integer countYearReceive(Long memId, String giftCode) {
        Date yearBeginTime = DateUtils.getYearStartTime();
        Date yearEndTime = DateUtils.getYearEndTime();
        int countYear = memGiftReceiveMapper.countVipTimeIntervalGift(memId, giftCode, yearBeginTime, yearEndTime);
        return countYear;
    }

    /**
     * 统计生日礼金
     *
     * @param memId
     * @param giftCode
     * @return
     */
    public Integer countBirthdayReceive(Long memId, String giftCode) {
        Date birthdayBeginTime = DateUtils.getYearStartTime();
        Date birthdayEndTime = DateUtils.getYearEndTime();
        int countBirthday = memGiftReceiveMapper.countVipTimeIntervalGift(memId, giftCode, birthdayBeginTime, birthdayEndTime);
        return countBirthday;
    }


}
