
package com.indo.user.common.kaptcha;

import lombok.Data;

/**
 * 登录验证码配置信息
 *
 * @author liaojinlong
 * @date 2020/6/10 18:53
 */
@Data
public class LoginCode {

    /**
     * 验证码配置
     */
    private LoginCodeEnum codeType;
    /**
     * 验证码有效期 分钟
     */
    private Long expiration = 2L;
    /**
     * 验证码内容长度
     */
    private int length = 4;
    /**
     * 验证码宽度
     */
    private int width = 111;
    /**
     * 验证码高度
     */
    private int height = 36;
    /**
     * 验证码字体
     */
    private String fontName;
    /**
     * 字体大小
     */
    private int fontSize = 25;

    public LoginCodeEnum getCodeType() {
        return codeType;
    }
}
