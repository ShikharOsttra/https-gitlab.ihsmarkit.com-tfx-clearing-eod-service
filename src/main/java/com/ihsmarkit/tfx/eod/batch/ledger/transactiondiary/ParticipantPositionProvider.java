package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;

import lombok.Getter;

@Service
@StepScope
public class ParticipantPositionProvider {

    @Getter
    private final Lazy<Map<String, Map<String, List<ParticipantPositionEntity>>>> participantPositions;

    public ParticipantPositionProvider(
        final ParticipantPositionRepository participantPositionRepository, @Value("#{jobParameters['businessDate']}") final LocalDate businessDate) {

        participantPositions = Lazy.of(() -> participantPositionRepository.findAllNonRebalancingByTradeDateFetchCurrencyPairAndParticipant(businessDate)
            .stream()
            .collect(Collectors.groupingBy(pp -> pp.getParticipant().getCode(),
                Collectors.groupingBy(pp -> pp.getCurrencyPair().getCode())))
        );
    }

    public Optional<ParticipantPositionEntity> getParticipantPositionByCurrencyPairAndParticipantCodeAndType(
        final String currencyPairCode, final String participantCode, final ParticipantPositionType type) {
        final List<ParticipantPositionEntity> participantPositions = this.participantPositions.get().get(participantCode).get(currencyPairCode);
        return participantPositions.stream()
            .filter(pp -> pp.getType() == type)
            .findFirst();
    }
}