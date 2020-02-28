package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class BuySellAmounts {

    private final BigDecimal buy;
    private final BigDecimal sell;
}
