package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.CacheConfig.JPY_RATES_CACHE;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.config.EodJobConstants;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class JPYRateService {

    private final DailySettlementPriceService dailySettlementPriceService;

    @Cacheable(JPY_RATES_CACHE)
    public BigDecimal getJpyRate(final LocalDate date, final String currency) {
        return dailySettlementPriceService.getPrice(date, currency, EodJobConstants.JPY);
    }
}
