package com.ihsmarkit.tfx.eod.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class SettlementDateProvider {

    @SuppressWarnings("checkstyle:MagicNumber")
    public LocalDate getSettlementDateFor(final LocalDate tradeDate) {
        //STUB IMPLEMENTATION
        return tradeDate.plusDays(3);
    }
}

