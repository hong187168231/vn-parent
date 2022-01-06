package com.indo.game.pojo.dto.sbo;

import lombok.Data;

@Data
public class SboCallBackLiveCoinTransactionReq extends SboCallBackParentReq{
    private String amount;
    private String selection;
    private String transcationTime;
    private String transferCode;
    private String transactionId;
}