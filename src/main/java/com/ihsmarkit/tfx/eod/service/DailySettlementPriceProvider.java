package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;

@Service
public class DailySettlementPriceProvider {

    // mock data which will be removed when we have DailySettlementPriceRepository ready in core-dl
    private static final Map<CurrencyPairEntity, BigDecimal> PRICE_MAP = Map.of(
        CurrencyPairEntity.of(0L, "USD", "JPY"), new BigDecimal("99.111"),
        CurrencyPairEntity.of(1L, "EUR", "JPY"), new BigDecimal("120.79"),
        CurrencyPairEntity.of(2L, "EUR", "USD"), new BigDecimal("1.1"),
        CurrencyPairEntity.of(81L, "USD", "EUR"), new BigDecimal("0.9")
    );

    public Map<CurrencyPairEntity, BigDecimal> getDailySettlementPrices(final LocalDate businessDate) {
        return PRICE_MAP;
    }
}
