package com.ihsmarkit.tfx.eod.model;

import java.time.LocalDate;

import lombok.Value;

@Value(staticConstructor = "of")
public class CurrencyPairKeyAndDate {
    private final Long currencyPairId;
    private final LocalDate date;
}
