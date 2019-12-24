package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class BalanceContribution {

    private final BigDecimal totalBalanceContribution;
    private final BigDecimal cashBalanceContribution;

}
