package com.ihsmarkit.tfx.eod.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.eod.config.EodJobConstants;
import com.ihsmarkit.tfx.eod.model.CashSettlement;

@Mapper(config = DefaultMapperConfig.class)
public interface CashSettlementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "participant", source = "settlement.participant")
    @Mapping(target = "type", source = "settlement.type")
    @Mapping(target = "dateType", source = "settlement.dateType")
    @Mapping(target = "amount.value", source = "settlement.amount")
    @Mapping(target = "amount.currency", constant = EodJobConstants.JPY)
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "date", source = "date")
    EodCashSettlementEntity toEntity(CashSettlement settlement, LocalDate date, LocalDateTime timestamp);
}
