package com.ihsmarkit.tfx.eod.mapper;

import java.time.LocalDate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.eod.config.EodJobConstants;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;

@Mapper(config = DefaultMapperConfig.class)
public interface ParticipantPositionForPairMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "participant", source = "trade.participant")
    @Mapping(target = "currencyPair", source = "trade.currencyPair")
    @Mapping(target = "date", source = "businessDate")
    @Mapping(target = "tlementDate", source = "settlementDate")
    @Mapping(target = "amount.value", source = "trade.amount")
    @Mapping(target = "amount.currency", constant = EodJobConstants.JPY)
    EodProductCashSettlementEntity toEodProductCashSettlement(
        ParticipantPositionForPair trade,
        LocalDate businessDate,
        LocalDate settlementDate,
        EodProductCashSettlementType type);
}
