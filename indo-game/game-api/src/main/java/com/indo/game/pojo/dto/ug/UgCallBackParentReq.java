package com.indo.game.pojo.dto.ug;

import com.alibaba.fastjson.annotation.JSONField;

public class UgCallBackParentReq {

    private String APIPassword;

    @JSONField(name="APIPassword")
    public String getAPIPassword() {
        return APIPassword;
    }

    @JSONField(name="APIPassword")
    public void setAPIPassword(String APIPassword) {
        this.APIPassword = APIPassword;
    }
}