package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioEntity;
import com.ihsmarkit.tfx.core.dl.entity.MarginRatioMultiplierEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.repository.MarginRatioMultiplierRepository;
import com.ihsmarkit.tfx.core.dl.repository.MarginRatioRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
@Getter(AccessLevel.PRIVATE)
public class MarginRatioService {

    private final MarginRatioRepository marginRatioRepository;
    private final MarginRatioMultiplierRepository marginRatioMultiplierRepository;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final Lazy<Map<CurrencyPairEntity, BigDecimal>> marginRatio = Lazy.of(
        () -> getMarginRatioRepository().findByBusinessDate(getBusinessDate()).stream()
            .collect(Collectors.toMap(MarginRatioEntity::getCurrencyPair, MarginRatioEntity::getValue))
    );
    private final Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> marginRatioMultiplier = new HashMap<>();

    public BigDecimal getRequiredMarginRatio(final CurrencyPairEntity currencyPair, final ParticipantEntity participant) {

        return marginRatioMultiplier.computeIfAbsent(
            participant,
            p -> marginRatioMultiplierRepository.findAllByBusinessDateAndParticipantCode(businessDate, participant.getCode()).stream()
                                .collect(Collectors.toMap(MarginRatioMultiplierEntity::getCurrencyPair, MarginRatioMultiplierEntity::getValue))
        ).get(currencyPair)
            .multiply(marginRatio.get().get(currencyPair));

    }
}
