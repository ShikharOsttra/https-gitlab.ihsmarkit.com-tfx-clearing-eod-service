package com.ihsmarkit.tfx.eod.model;

import java.time.LocalDate;

import lombok.Value;
import lombok.ToString;

@Value(staticConstructor = "of")
@ToString
public class CurrencyPairKeyAndDate {
    private final String baseCurrency;
    private final String valueCurrency;
    private final LocalDate date;

    public static CurrencyPairKeyAndDate of(final CurrencyPairEntity currencyPair, final LocalDate date) {
        return of(currencyPair.getBaseCurrency(), currencyPair.getBaseCurrency(), date);
    }
}
