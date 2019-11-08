package com.ihsmarkit.tfx.eod.service;

import static com.google.common.collect.Maps.newHashMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;

@SuppressWarnings({ "checkstyle:MagicNumber", "PMD.AvoidDuplicateLiterals", "PMD.NonStaticInitializer" })
@Service
public class DailySettlementPriceProvider {

    // mock data which will be removed when we have DailySettlementPriceRepository ready in core-dl
    private static final Map<CurrencyPairEntity, BigDecimal> PRICE_MAP = newHashMap();
    {
        PRICE_MAP.put(CurrencyPairEntity.of(1L, "USD", "JPY"), new BigDecimal("109.38"));
        PRICE_MAP.put(CurrencyPairEntity.of(2L, "EUR", "JPY"), new BigDecimal("120.66"));
        PRICE_MAP.put(CurrencyPairEntity.of(3L, "GBP", "JPY"), new BigDecimal("139.99"));
        PRICE_MAP.put(CurrencyPairEntity.of(4L, "AUD", "JPY"), new BigDecimal("75.08"));
        PRICE_MAP.put(CurrencyPairEntity.of(5L, "CHF", "JPY"), new BigDecimal("109.73"));
        PRICE_MAP.put(CurrencyPairEntity.of(6L, "CAD", "JPY"), new BigDecimal("82.87"));
        PRICE_MAP.put(CurrencyPairEntity.of(7L, "NZD", "JPY"), new BigDecimal("69.31"));
        PRICE_MAP.put(CurrencyPairEntity.of(8L, "ZAR", "JPY"), new BigDecimal("7.37"));
        PRICE_MAP.put(CurrencyPairEntity.of(9L, "TRY", "JPY"), new BigDecimal("19.02"));
        PRICE_MAP.put(CurrencyPairEntity.of(10L, "NOK", "JPY"), new BigDecimal("11.94"));
        PRICE_MAP.put(CurrencyPairEntity.of(11L, "HKD", "JPY"), new BigDecimal("13.97"));
        PRICE_MAP.put(CurrencyPairEntity.of(12L, "SEK", "JPY"), new BigDecimal("11.29"));
        PRICE_MAP.put(CurrencyPairEntity.of(13L, "EUR", "USD"), new BigDecimal("1.10"));
        PRICE_MAP.put(CurrencyPairEntity.of(14L, "GBP", "USD"), new BigDecimal("1.28"));
        PRICE_MAP.put(CurrencyPairEntity.of(15L, "GBP", "CHF"), new BigDecimal("1.28"));
        PRICE_MAP.put(CurrencyPairEntity.of(16L, "USD", "CHF"), new BigDecimal("1.00"));
        PRICE_MAP.put(CurrencyPairEntity.of(17L, "USD", "CAD"), new BigDecimal("1.32"));
        PRICE_MAP.put(CurrencyPairEntity.of(18L, "AUD", "USD"), new BigDecimal("0.69"));
        PRICE_MAP.put(CurrencyPairEntity.of(19L, "EUR", "CHF"), new BigDecimal("1.10"));
        PRICE_MAP.put(CurrencyPairEntity.of(20L, "EUR", "GBP"), new BigDecimal("0.86"));
        PRICE_MAP.put(CurrencyPairEntity.of(21L, "NZD", "USD"), new BigDecimal("0.63"));
        PRICE_MAP.put(CurrencyPairEntity.of(22L, "EUR", "AUD"), new BigDecimal("1.61"));
        PRICE_MAP.put(CurrencyPairEntity.of(23L, "GBP", "AUD"), new BigDecimal("1.86"));
        PRICE_MAP.put(CurrencyPairEntity.of(24L, "MXN", "JPY"), new BigDecimal("5.70"));
        PRICE_MAP.put(CurrencyPairEntity.of(25L, "AUD", "CHF"), new BigDecimal("0.68"));
        PRICE_MAP.put(CurrencyPairEntity.of(26L, "AUD", "NZD"), new BigDecimal("1.08"));
        PRICE_MAP.put(CurrencyPairEntity.of(27L, "NZD", "CHF"), new BigDecimal("0.63"));
        PRICE_MAP.put(CurrencyPairEntity.of(28L, "USD", "HKD"), new BigDecimal("7.83"));
        PRICE_MAP.put(CurrencyPairEntity.of(29L, "CNH", "JPY"), new BigDecimal("15.67"));
        PRICE_MAP.put(CurrencyPairEntity.of(30L, "SGD", "JPY"), new BigDecimal("80.48"));
        PRICE_MAP.put(CurrencyPairEntity.of(31L, "AUD", "CAD"), new BigDecimal("0.91"));
        PRICE_MAP.put(CurrencyPairEntity.of(32L, "EUR", "CAD"), new BigDecimal("1.45"));
        PRICE_MAP.put(CurrencyPairEntity.of(33L, "CAD", "CHF"), new BigDecimal("0.76"));
    }

    public Map<CurrencyPairEntity, BigDecimal> getDailySettlementPrices(final LocalDate businessDate) {
        return PRICE_MAP;
    }
}
