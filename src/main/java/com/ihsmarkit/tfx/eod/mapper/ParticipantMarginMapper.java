package com.ihsmarkit.tfx.eod.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.annotation.Nullable;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantMarginEntity;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;

@Mapper(config = DefaultMapperConfig.class)
public interface ParticipantMarginMapper {

    String UNWRAP_BIG_DECIMAL = "UNWRAP_BIG_DECIMAL";
    String UNWRAP = "UNWRAP";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "participant", source = "margin.participant")
    @Mapping(target = "requiredAmount", source = "margin.requiredAmount", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "initialMargin", source = "margin.initialMargin", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "marginRatio", source = "margin.marginRatio", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "marginAlertLevel", source = "margin.marginAlertLevel", qualifiedByName = UNWRAP)
    @Mapping(target = "totalDeficit", source = "margin.totalDeficit", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "cashDeficit", source = "margin.cashDeficit", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "cashCollateral", source = "margin.cashCollateralAmount", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "logCollateral", source = "margin.logCollateralAmount", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "pnl", source = "margin.pnl", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "todaySettlement", source = "margin.todaySettlement", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "nextDaySettlement", source = "margin.nextDaySettlement", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "date", source = "date")
    EodParticipantMarginEntity toEntity(ParticipantMargin margin, LocalDate date, LocalDateTime timestamp);

    @Named(UNWRAP_BIG_DECIMAL)
    default BigDecimal unwrapBigDecimal(Optional<BigDecimal> input) {
        return input.orElse(BigDecimal.ZERO);
    }

    @Nullable
    @Named(UNWRAP)
    default <T> T unwrap(Optional<T> input) {
        return input.orElse(null);
    }
}
