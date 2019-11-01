package com.ihsmarkit.tfx.eod.mtm;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;

@Mapper(config = DefaultMapperConfig.class)
public interface TradeOrPositionEssentialsMapper {

    @Mapping(target = "currencyPair", source = "currencyPair")
    @Mapping(target = "participant", source = "originator.participant")
    @Mapping(target = "baseAmount", expression = "java( trade.getBaseAmount().getValue().multiply(java.math.BigDecimal.valueOf(trade.getDirection()==com.ihsmarkit.tfx.core.domain.type.Side.SELL?-1l:1l)))")
    @Mapping(target = "spotRate", source = "spotRate")
    TradeOrPositionEssentials convertTrade(TradeEntity trade);

    @Mapping(target = "currencyPair", source = "currencyPair")
    @Mapping(target = "participant", source = "participant")
    @Mapping(target = "baseAmount", source = "amount.value")
    @Mapping(target = "spotRate", source = "price")
    TradeOrPositionEssentials convertPosition(ParticipantPositionEntity position);

}
