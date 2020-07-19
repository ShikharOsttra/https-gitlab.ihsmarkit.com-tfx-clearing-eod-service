package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@JobScope
@RequiredArgsConstructor
@Slf4j
public class DailySettlementPriceService {

    private final DailySettlementPriceRepository dailySettlementPriceRepository;

    private final Map<LocalDate, Map<String, BigDecimal>> dailySettlementPrices = new ConcurrentHashMap<>();

    public BigDecimal getPrice(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return Optional.ofNullable(getPrice(date, currencyPair.getBaseCurrency(), currencyPair.getValueCurrency()))
            .orElseGet(() -> {
                log.error("unable to find price for currencyPair: {} on date: {}", currencyPair, date);
                return null;
            });
    }

    public BigDecimal getPrice(final LocalDate date, final String baseCurrency, final String valueCurrency) {
        final var currencyPairKey = baseCurrency + valueCurrency;
        return dailySettlementPrices.computeIfAbsent(
            date,
            businessDate -> dailySettlementPriceRepository.findLatestDailySettlementPrices(businessDate).stream()
                .filter(entity -> entity.getDailySettlementPrice() != null)
                .collect(
                    Collectors.toMap(
                        dsp -> toCurrencyPairKey(dsp.getCurrencyPair()),
                        DailySettlementPriceEntity::getDailySettlementPrice
                    )
                )
        )
            .get(currencyPairKey);
    }

    private static String toCurrencyPairKey(final CurrencyPairEntity currencyPairEntity) {
        return currencyPairEntity.getBaseCurrency() + currencyPairEntity.getValueCurrency();
    }
}
