package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class DailySettlementPriceService {

    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    public BigDecimal getPrice(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return dailySettlementPriceProvider.getDailySettlementPrices(date)
            .get(currencyPair.getBaseCurrency() + currencyPair.getValueCurrency());
    }

    public BigDecimal getPrice(final LocalDate date, final String baseCurrency, final String valueCurrency) {
        return dailySettlementPriceProvider.getDailySettlementPrices(date).get(baseCurrency + valueCurrency);
    }


}
