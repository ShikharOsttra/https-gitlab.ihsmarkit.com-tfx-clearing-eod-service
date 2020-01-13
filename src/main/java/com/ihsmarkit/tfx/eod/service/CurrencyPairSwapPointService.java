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

    private final Map<LocalDate, Map<CurrencyPairEntity, BigDecimal>> swapPoints = new ConcurrentHashMap<>();

    private final EodSwapPointRepository eodSwapPointRepository;

    public BigDecimal getSwapPoint(final LocalDate date, final CurrencyPairEntity currencyPair) {
        return swapPoints.computeIfAbsent(
            date,
            businessDate -> eodSwapPointRepository.findAllByDateOrderedByProductNumber(date).stream()
                .filter(swapPoint -> swapPoint.getSwapPointDays() != 0)
                .collect(Collectors.toMap(EodSwapPointEntity::getCurrencyPair, EodSwapPointEntity::getSwapPoint))
        ).get(currencyPair);
    }
}
