package com.indo.user.controller;


import com.indo.admin.pojo.vo.act.AdvertiseVO;
import com.indo.common.annotation.AllowAccess;
import com.indo.common.constant.RedisConstants;
import com.indo.common.result.Result;
import com.indo.common.web.util.DozerUtil;
import com.indo.core.pojo.entity.Advertise;
import com.indo.user.common.util.UserBusinessRedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 活动 前端控制器
 * </p>
 *
 * @author puff
 * @since 2021-11-17
 */
@Api(tags = "app广告接口")
@RestController
@RequestMapping("/api/v1/users/ade")
public class AdvertisingController {

    @Resource
    private DozerUtil dozerUtil;

    @ApiOperation(value = "查询广告列表", httpMethod = "GET")
    @GetMapping(value = "/list")
    @AllowAccess
    public Result<List<AdvertiseVO>> adeList() {
        Map<Object, Object> map = UserBusinessRedisUtils.hmget(RedisConstants.ADMIN_ADVERTISING_KEY);
        List<Advertise> advertiseList = new ArrayList(map.values());
        List<AdvertiseVO> advertiseVOList = dozerUtil.convert(advertiseList, AdvertiseVO.class);
        return Result.success(advertiseVOList);
    }

}
