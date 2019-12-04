package com.ihsmarkit.tfx.eod.model;

import java.time.LocalDate;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;

import lombok.Value;

@Value(staticConstructor = "of")
public class CurrencyPairKeyAndDate {
    private final String baseCurrency;
    private final String valueCurrency;
    private final LocalDate date;

    public static CurrencyPairKeyAndDate of(final CurrencyPairEntity currencyPair, final LocalDate date) {
        return of(currencyPair.getBaseCurrency(), currencyPair.getValueCurrency(), date);
    }
}
