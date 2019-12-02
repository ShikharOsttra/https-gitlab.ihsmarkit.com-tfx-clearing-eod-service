package com.ihsmarkit.tfx.eod.service;

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
public class DailySettlementPriceService {

    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    @Cacheable(value = "dsp", key = "T(com.ihsmarkit.tfx.eod.model.CurrencyPairKeyAndDate).of(#currencyPair, #date)")
    public BigDecimal getPrice(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return dailySettlementPriceProvider.getDailySettlementPrices(date)
            .get(currencyPair.getBaseCurrency() + currencyPair.getValueCurrency());
    }

    @Cacheable(value = "dsp", key = "T(com.ihsmarkit.tfx.eod.model.CurrencyPairKeyAndDate).of(#baseCurrency, #valueCurrency, #date)")
    public BigDecimal getPrice(final LocalDate date, final String baseCurrency, final String valueCurrency) {
        return dailySettlementPriceProvider.getDailySettlementPrices(date).get(baseCurrency + valueCurrency);
    }


}
