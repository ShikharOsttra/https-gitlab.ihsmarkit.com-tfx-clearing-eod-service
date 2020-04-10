package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.config.EodJobConstants;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class JPYRateService {

    private final DailySettlementPriceService dailySettlementPriceService;

    public BigDecimal getJpyRate(final LocalDate date, final String currency) {
        return dailySettlementPriceService.getPrice(date, currency, EodJobConstants.JPY);
    }
}
