package com.indo.game.pojo.dto.bti;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class BtiReserveRequst {
    private String cust_id;

    private String reserve_id;

    private BigDecimal amount;

    private String extsessionID;
}
