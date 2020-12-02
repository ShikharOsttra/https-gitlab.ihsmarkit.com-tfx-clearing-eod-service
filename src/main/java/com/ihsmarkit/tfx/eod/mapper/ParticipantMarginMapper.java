package com.ihsmarkit.tfx.eod.mapper;

import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.CASH_DRAWABLE_AMOUNT_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.CASH_SURPLUS_DEFICIENCY_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.DRAWABLE_AMOUNT_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.MARGIN_AMOUNT_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.MARGIN_CALL_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.REQUIRED_MARGIN_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.SURPLUS_DEFICIENCY_MAPPER;
import static com.ihsmarkit.tfx.core.margin.MarginCalculationMapper.SURPLUS_MAPPER;

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
import com.ihsmarkit.tfx.core.margin.MarginAdapter;
import com.ihsmarkit.tfx.core.margin.MarginCalculationMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantMargin;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Mapper(config = DefaultMapperConfig.class, uses = MarginCalculationMapper.class)
public interface ParticipantMarginMapper {

    String UNWRAP_BIG_DECIMAL = "UNWRAP_BIG_DECIMAL";
    String UNWRAP = "UNWRAP";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "participant", source = "margin.participant")
    @Mapping(target = "requiredAmount", source = "margin", qualifiedByName = REQUIRED_MARGIN_MAPPER)
    @Mapping(target = "initialMargin", source = "margin.initialMargin", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "marginRatio", source = "margin.marginRatio", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "marginAlertLevel", source = "margin.marginAlertLevel", qualifiedByName = UNWRAP)
    @Mapping(target = "totalDeficit", source = "margin", qualifiedByName = SURPLUS_DEFICIENCY_MAPPER)
    @Mapping(target = "cashDeficit", source = "margin", qualifiedByName = CASH_SURPLUS_DEFICIENCY_MAPPER)
    @Mapping(target = "cashCollateral", source = "margin.cashCollateralAmount", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "logCollateral", source = "margin.logCollateralAmount", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "pnl", source = "margin.pnl", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "todaySettlement", source = "margin.todaySettlement", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "nextDaySettlement", source = "margin.nextDaySettlement", qualifiedByName = UNWRAP_BIG_DECIMAL)
    @Mapping(target = "marginAmount", source = "margin", qualifiedByName = MARGIN_AMOUNT_MAPPER)
    @Mapping(target = "surplus", source = "margin", qualifiedByName = SURPLUS_MAPPER)
    @Mapping(target = "drawableAmount", source = "margin", qualifiedByName = DRAWABLE_AMOUNT_MAPPER)
    @Mapping(target = "cashDrawableAmount", source = "margin", qualifiedByName = CASH_DRAWABLE_AMOUNT_MAPPER)
    @Mapping(target = "marginCall", source = "margin", qualifiedByName = MARGIN_CALL_MAPPER)
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "date", source = "date")
    EodParticipantMarginEntity toEntity(ParticipantMargin margin, LocalDate date, LocalDateTime timestamp);

    @Named(UNWRAP_BIG_DECIMAL)
    default BigDecimal unwrapBigDecimal(Optional<BigDecimal> input) {
        return input.orElse(BigDecimal.ZERO);
    }

    default MarginAdapter mapToMarginAdapter(ParticipantMargin margin) {
        return MarginAdapterImpl.of(margin);
    }

    @Nullable
    @Named(UNWRAP)
    default <T> T unwrap(Optional<T> input) {
        return input.orElse(null);
    }
}
