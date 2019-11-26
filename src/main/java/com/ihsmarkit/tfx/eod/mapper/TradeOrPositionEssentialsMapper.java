package com.ihsmarkit.tfx.eod.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

@Mapper(config = DefaultMapperConfig.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface TradeOrPositionEssentialsMapper {

    String BASE_AMOUNT_TO_SIGNED_VALUE_MAPPER = "mapBaseAmountToSignedValue";

    @SuppressWarnings("checkstyle:LineLength")
    @Mapping(target = "currencyPair", source = "currencyPair")
    @Mapping(target = "participant", source = "originator.participant")
    @Mapping(target = "amount", source = "trade", qualifiedByName = BASE_AMOUNT_TO_SIGNED_VALUE_MAPPER)
    @Mapping(target = "spotRate", source = "spotRate")
    TradeOrPositionEssentials convertTrade(TradeEntity trade);

    @Mapping(target = "currencyPair", source = "currencyPair")
    @Mapping(target = "participant", source = "participant")
    @Mapping(target = "amount", source = "amount.value")
    @Mapping(target = "spotRate", source = "price")
    TradeOrPositionEssentials convertPosition(ParticipantPositionEntity position);

    @Named(BASE_AMOUNT_TO_SIGNED_VALUE_MAPPER)
    default BigDecimal mapBaseAmountToSignedValue(TradeEntity trade) {
        final BigDecimal baseAmountValue = trade.getBaseAmount().getValue();
        return trade.getDirection().isSell() ? baseAmountValue.negate() : baseAmountValue;
    }
}
