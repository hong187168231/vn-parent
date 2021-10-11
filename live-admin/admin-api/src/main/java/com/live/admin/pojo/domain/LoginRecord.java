package com.live.admin.pojo.domain;

import com.live.common.base.BaseDocument;
import lombok.Data;

/**
 * @author 
 * @date 2021-03-09
 */
@Data
public class LoginRecord extends BaseDocument {

    private String clientIP;

    private long elapsedTime;

    private Object message;

    private String token;

    private String username;

    private String loginTime;

    private String region;

    /**
     * 会话状态 0-离线 1-在线
     */
    private Integer status;

}
