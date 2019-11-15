package com.ihsmarkit.tfx.eod.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.config.EodJobConstants;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;

@Mapper(config = DefaultMapperConfig.class)
public interface ParticipantPositionForPairMapper {

    String PARTICIPANT_POS_FOR_PAIR_AMOUNT_CONVERTOR = "participantPOsForPairAmountConvertor";

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
        EodProductCashSettlementType type
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "price", source = "price")
    @Mapping(target = "participant", source = "trade.participant")
    @Mapping(target = "participantType", source = "trade.participant.type")
    @Mapping(target = "currencyPair", source = "trade.currencyPair")
    @Mapping(target = "amount", source = "trade", qualifiedByName = PARTICIPANT_POS_FOR_PAIR_AMOUNT_CONVERTOR)
    @Mapping(target = "type", source = "positionType")
    @Mapping(target = "tradeDate", source = "businessDate")
    @Mapping(target = "valueDate", source = "settlementDate")
    ParticipantPositionEntity toParticipantPosition(
        ParticipantPositionForPair trade,
        ParticipantPositionType positionType,
        LocalDate businessDate,
        LocalDate settlementDate,
        BigDecimal price
    );

    @Named(PARTICIPANT_POS_FOR_PAIR_AMOUNT_CONVERTOR)
    default AmountEntity mapAmount(ParticipantPositionForPair trade) {
        return AmountEntity.of(trade.getAmount(), trade.getCurrencyPair().getBaseCurrency());
    }
}
