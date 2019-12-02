package com.ihsmarkit.tfx.eod.model;

import java.time.LocalDate;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
@ToString
public class CurrencyPairKeyAndDate {
    private final String baseCurrency;
    private final String valueCurrency;
    private final LocalDate date;

    public static CurrencyPairKeyAndDate of(final CurrencyPairEntity currencyPair, final LocalDate date) {
        return of(currencyPair.getBaseCurrency(), currencyPair.getBaseCurrency(), date);
    }
}
