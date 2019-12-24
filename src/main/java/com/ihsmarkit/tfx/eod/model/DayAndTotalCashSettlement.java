package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DayAndTotalCashSettlement {
    private final Optional<BigDecimal> day;
    private final BigDecimal total;
}
