package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CashSettlement {
    private final ParticipantEntity participant;
    private EodProductCashSettlementType type;
    private EodCashSettlementDateType dateType;
    private BigDecimal amount;
}
