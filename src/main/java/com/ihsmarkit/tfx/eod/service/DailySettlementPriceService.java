package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class DailySettlementPriceService {

    private final DailySettlementPriceRepository dailySettlementPriceRepository;

    private final Map<LocalDate, Map<String, BigDecimal>> dailySettlementPrices = new ConcurrentHashMap<>();

    public BigDecimal getPrice(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return getPrice(date, currencyPair.getBaseCurrency(), currencyPair.getValueCurrency());
    }

    public BigDecimal getPrice(final LocalDate date, final String baseCurrency, final String valueCurrency) {
        return dailySettlementPrices.computeIfAbsent(
            date,
            businessDate -> dailySettlementPriceRepository.findAllByBusinessDate(date).stream()
                .filter(entity -> entity.getDailySettlementPrice() != null)
                .collect(
                    Collectors.toMap(
                        dsp -> dsp.getCurrencyPair().getBaseCurrency() + dsp.getCurrencyPair().getValueCurrency(),
                        DailySettlementPriceEntity::getDailySettlementPrice
                    )
                )
        ).get(baseCurrency + valueCurrency);
    }
}
