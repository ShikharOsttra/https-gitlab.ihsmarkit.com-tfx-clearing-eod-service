package com.ihsmarkit.tfx.eod.model;

import java.time.LocalDate;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
public class CurrencyPairKeyAndDate {
    private final Long currencyPairId;
    private final LocalDate date;
}
