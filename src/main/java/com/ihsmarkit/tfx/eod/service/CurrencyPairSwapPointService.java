package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.EodSwapPointEntity;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.EodSwapPointRepository;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class CurrencyPairSwapPointService {

    private final Map<LocalDate, Map<String, BigDecimal>> swapPoints = new ConcurrentHashMap<>();

    private final EodSwapPointRepository eodSwapPointRepository;

    private final static BigDecimal ZERO_SWAP_POINTS = BigDecimal.ZERO.setScale(3);

    public BigDecimal getSwapPoint(final LocalDate date, final String currencyPair) {
        return swapPoints.computeIfAbsent(
            date,
            businessDate -> eodSwapPointRepository.findAllByDateOrderedByProductNumber(date).stream()
                .filter(swapPoint -> swapPoint.getSwapPointDays() != 0)
                .collect(Collectors.toMap(item -> item.getCurrencyPair().getCode(), EodSwapPointEntity::getSwapPoint))
        ).getOrDefault(currencyPair, ZERO_SWAP_POINTS);
    }

    public BigDecimal getSwapPoint(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return getSwapPoint(date, currencyPair.getCode());
    }
}
