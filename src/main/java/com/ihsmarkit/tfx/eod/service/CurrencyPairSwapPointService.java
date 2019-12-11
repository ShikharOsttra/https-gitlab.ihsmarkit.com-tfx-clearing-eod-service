package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.CacheConfig.SWAP_POINT_CACHE;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class CurrencyPairSwapPointService {

    private final CurrencyPairSwapPointMockProvider currencyPairSwapPointMockProvider;

    @Cacheable(value = SWAP_POINT_CACHE, key = "T(com.ihsmarkit.tfx.eod.model.CurrencyPairKeyAndDate).of(#currencyPair, #date)")
    public BigDecimal getSwapPoint(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return currencyPairSwapPointMockProvider.getCurrencySwapPoint(date, currencyPair);
    }
}
