package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class BuySellAmounts {
    private final Optional<BigDecimal> buy;
    private final Optional<BigDecimal> sell;
}
