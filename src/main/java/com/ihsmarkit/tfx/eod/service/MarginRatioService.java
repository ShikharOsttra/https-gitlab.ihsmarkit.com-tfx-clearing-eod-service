package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import lombok.extern.slf4j.Slf4j;

@Component
@JobScope
@RequiredArgsConstructor
@Slf4j
@Getter(AccessLevel.PRIVATE)
public class MarginRatioService {

    private static final int MARGIN_RATIO_SCALE = 2;
    private static final RoundingMode MARGIN_RATIO_ROUNDING = RoundingMode.HALF_UP;

    private final MarginRatioRepository marginRatioRepository;
    private final MarginRatioMultiplierRepository marginRatioMultiplierRepository;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final Lazy<Map<CurrencyPairEntity, BigDecimal>> marginRatio = Lazy.of(
        () -> getMarginRatioRepository().findByBusinessDate(getBusinessDate()).stream()
            .collect(Collectors.toMap(MarginRatioEntity::getCurrencyPair, MarginRatioEntity::getValue))
    );
    private final Map<ParticipantEntity, Map<CurrencyPairEntity, BigDecimal>> marginRatioMultiplier = new ConcurrentHashMap<>();

    public BigDecimal getRequiredMarginRatio(final CurrencyPairEntity currencyPair, final ParticipantEntity participant) {

        return marginRatioMultiplier.computeIfAbsent(
            participant,
            p -> getMarginRatioMultipliers(participant)
        ).computeIfAbsent(currencyPair, pairEntity -> {
            throw new IllegalStateException(String.format("unable to find margin multiplier for currency pair: %s, participant: %s and businessDate: %s",
                currencyPair.getCode(), participant.getCode(), businessDate));
        })
            .multiply(marginRatio.get().get(currencyPair))
            .setScale(MARGIN_RATIO_SCALE, MARGIN_RATIO_ROUNDING);
    }

    private Map<CurrencyPairEntity, BigDecimal> getMarginRatioMultipliers(final ParticipantEntity participant) {
        try {
            return marginRatioMultiplierRepository.findAllByBusinessDateAndParticipantCode(businessDate, participant.getCode()).stream()
                                .collect(Collectors.toMap(MarginRatioMultiplierEntity::getCurrencyPair,
                                                          MarginRatioMultiplierEntity::getValue));
        } catch (final Exception ex) {
            log.error("unable to build margin ratio multipliers map for participant: {} with message: {}", participant.getCode(), ex.getMessage());
            throw ex;
        }
    }
}
