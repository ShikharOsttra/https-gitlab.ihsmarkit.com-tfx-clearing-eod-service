package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.config.EodJobConstants;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class JPYRatesService {

    private final DailySettlementPriceService dailySettlementPriceService;

    public BigDecimal getJpyRate(final LocalDate date, final String currency) {
        return dailySettlementPriceService.getPrice(date, currency, EodJobConstants.JPY);
    }
}
