package com.indo.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.indo.common.pojo.bo.LoginInfo;
import com.indo.core.pojo.entity.PayChannelConfig;
import com.indo.core.pojo.entity.PayWayConfig;
import com.indo.pay.pojo.vo.PayWayVO;

import java.util.List;

/**
 * <p>
 * 支付方式配置 服务类
 * </p>
 *
 * @author puff
 * @since 2021-11-13
 */
public interface IPayWayService extends IService<PayWayConfig> {


    List<PayWayVO> wayList(LoginInfo loginInfo, Long payChannelId);

    PayWayConfig getPayWayById(Long wayId);

}
