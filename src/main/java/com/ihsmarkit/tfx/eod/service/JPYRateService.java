package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.CacheConfig.JPY_CROSS_RATES_CACHE;
import static com.ihsmarkit.tfx.eod.config.CacheConfig.JPY_RATES_CACHE;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.JPY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@JobScope
@RequiredArgsConstructor
@Slf4j
public class JPYRateService {

    private final DailySettlementPriceService dailySettlementPriceService;

    @Cacheable(JPY_RATES_CACHE)
    public BigDecimal getJpyRate(final LocalDate date, final String currency) {
        return Optional.ofNullable(dailySettlementPriceService.getPrice(date, currency, JPY)).orElseGet(() -> {
            log.error("unable to find JPY price for currency: {} on date: {}", currency, date);
            return null;
        });
    }

    @Cacheable(JPY_CROSS_RATES_CACHE)
    public BigDecimal getJpyRate(final LocalDate date, final String baseCurrency, final String valueCurrency) {
        if (JPY.equals(valueCurrency)) {
            return getJpyRate(date, baseCurrency);
        } else {
            return getJpyRate(date, valueCurrency).multiply(dailySettlementPriceService.getPrice(date, baseCurrency, valueCurrency));
        }
    }
}
