package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
@SuppressWarnings({ "checkstyle:MagicNumber", "PMD.AvoidDuplicateLiterals", "PMD.NonStaticInitializer"})
@Service
public class CurrencyPairSwapPointMockProvider {
    private static final Map<CurrencyPairEntity, BigDecimal> SWAP_POINT = new HashMap<>();
    static {
        SWAP_POINT.put(CurrencyPairEntity.of(1L, "USD", "JPY"), new BigDecimal("80"));
        SWAP_POINT.put(CurrencyPairEntity.of(2L, "EUR", "JPY"), new BigDecimal("-5"));
        SWAP_POINT.put(CurrencyPairEntity.of(3L, "GBP", "JPY"), new BigDecimal("34"));
        SWAP_POINT.put(CurrencyPairEntity.of(4L, "AUD", "JPY"), new BigDecimal("38"));
        SWAP_POINT.put(CurrencyPairEntity.of(5L, "CHF", "JPY"), new BigDecimal("-14"));
        SWAP_POINT.put(CurrencyPairEntity.of(6L, "CAD", "JPY"), new BigDecimal("37"));
        SWAP_POINT.put(CurrencyPairEntity.of(7L, "NZD", "JPY"), new BigDecimal("35"));
        SWAP_POINT.put(CurrencyPairEntity.of(8L, "ZAR", "JPY"), new BigDecimal("113"));
        SWAP_POINT.put(CurrencyPairEntity.of(9L, "TRY", "JPY"), new BigDecimal("114"));
        SWAP_POINT.put(CurrencyPairEntity.of(10L, "NOK", "JPY"), new BigDecimal("28"));
        SWAP_POINT.put(CurrencyPairEntity.of(11L, "HKD", "JPY"), new BigDecimal("74"));
        SWAP_POINT.put(CurrencyPairEntity.of(12L, "SEK", "JPY"), new BigDecimal("-3"));
        SWAP_POINT.put(CurrencyPairEntity.of(13L, "EUR", "USD"), new BigDecimal("-0.86"));
        SWAP_POINT.put(CurrencyPairEntity.of(14L, "GBP", "USD"), new BigDecimal("-0.54"));
        SWAP_POINT.put(CurrencyPairEntity.of(15L, "GBP", "CHF"), new BigDecimal("0.51"));
        SWAP_POINT.put(CurrencyPairEntity.of(16L, "USD", "CHF"), new BigDecimal("0.82"));
        SWAP_POINT.put(CurrencyPairEntity.of(17L, "USD", "CAD"), new BigDecimal("0.32"));
        SWAP_POINT.put(CurrencyPairEntity.of(18L, "AUD", "USD"), new BigDecimal("-0.16"));
        SWAP_POINT.put(CurrencyPairEntity.of(19L, "EUR", "CHF"), new BigDecimal("0.1"));
        SWAP_POINT.put(CurrencyPairEntity.of(20L, "EUR", "GBP"), new BigDecimal("-0.28"));
        SWAP_POINT.put(CurrencyPairEntity.of(21L, "NZD", "USD"), new BigDecimal("-0.16"));
        SWAP_POINT.put(CurrencyPairEntity.of(22L, "EUR", "AUD"), new BigDecimal("-0.81"));
        SWAP_POINT.put(CurrencyPairEntity.of(23L, "GBP", "AUD"), new BigDecimal("-0.33"));
        SWAP_POINT.put(CurrencyPairEntity.of(24L, "MXN", "JPY"), new BigDecimal("112"));
        SWAP_POINT.put(CurrencyPairEntity.of(25L, "AUD", "CHF"), new BigDecimal("0.68"));
        SWAP_POINT.put(CurrencyPairEntity.of(26L, "AUD", "NZD"), new BigDecimal("1.08"));
        SWAP_POINT.put(CurrencyPairEntity.of(27L, "NZD", "CHF"), new BigDecimal("0.63"));
        SWAP_POINT.put(CurrencyPairEntity.of(28L, "USD", "HKD"), new BigDecimal("7.83"));
        SWAP_POINT.put(CurrencyPairEntity.of(29L, "CNH", "JPY"), new BigDecimal("15.67"));
        SWAP_POINT.put(CurrencyPairEntity.of(30L, "SGD", "JPY"), new BigDecimal("80.48"));
        SWAP_POINT.put(CurrencyPairEntity.of(31L, "AUD", "CAD"), new BigDecimal("0.91"));
        SWAP_POINT.put(CurrencyPairEntity.of(32L, "EUR", "CAD"), new BigDecimal("1.45"));
        SWAP_POINT.put(CurrencyPairEntity.of(33L, "CAD", "CHF"), new BigDecimal("0.76"));
        SWAP_POINT.put(CurrencyPairEntity.of(30L, "SGD", "JPY"), new BigDecimal("80.48"));
        SWAP_POINT.put(CurrencyPairEntity.of(31L, "AUD", "CAD"), new BigDecimal("0.91"));
        SWAP_POINT.put(CurrencyPairEntity.of(32L, "EUR", "CAD"), new BigDecimal("1.45"));
        SWAP_POINT.put(CurrencyPairEntity.of(33L, "CAD", "CHF"), new BigDecimal("0.76"));
    }

    public BigDecimal getCurrencySwapPoint(final LocalDate businessDate, final CurrencyPairEntity currencyPair) {
        return SWAP_POINT.get(currencyPair);
    }
}
