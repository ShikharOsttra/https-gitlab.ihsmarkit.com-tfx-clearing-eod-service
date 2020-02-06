package com.ihsmarkit.tfx.eod.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantMarginEntity;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;

@Mapper(config = DefaultMapperConfig.class)
public interface ParticipantMarginMapper {

    String UNWRAP = "unwrap";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "participant", source = "margin.participant")
    @Mapping(target = "requiredAmount", source = "margin.requiredAmount", qualifiedByName = UNWRAP)
    @Mapping(target = "initialMargin", source = "margin.initialMargin", qualifiedByName = UNWRAP)
    @Mapping(target = "totalDeficit", source = "margin.totalDeficit", qualifiedByName = UNWRAP)
    @Mapping(target = "cashDeficit", source = "margin.cashDeficit", qualifiedByName = UNWRAP)
    @Mapping(target = "cashCollateral", source = "margin.cashCollateralAmount", qualifiedByName = UNWRAP)
    @Mapping(target = "logCollateral", source = "margin.logCollateralAmount", qualifiedByName = UNWRAP)
    @Mapping(target = "pnl", source = "margin.pnl", qualifiedByName = UNWRAP)
    @Mapping(target = "todaySettlement", source = "margin.todaySettlement", qualifiedByName = UNWRAP)
    @Mapping(target = "nextDaySettlement", source = "margin.nextDaySettlement", qualifiedByName = UNWRAP)
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "date", source = "date")
    EodParticipantMarginEntity toEntity(ParticipantMargin margin, LocalDate date, LocalDateTime timestamp);

    @Named(UNWRAP)
    default BigDecimal unwrap(Optional<BigDecimal> input) {
        return input.orElse(BigDecimal.ZERO);
    }

}
