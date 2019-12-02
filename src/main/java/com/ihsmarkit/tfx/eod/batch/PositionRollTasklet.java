package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantPositionForPairMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class PositionRollTasklet implements Tasklet {

    private final ParticipantPositionRepository participantPositionRepository;

    private final EODCalculator eodCalculator;

    private final ParticipantPositionForPairMapper participantPositionForPairMapper;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

        final Stream<ParticipantPositionEntity> aggregated = eodCalculator
            .aggregatePositions(
                participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(businessDate)
            ).map(this::mapToParticipantPositionEntity);

        participantPositionRepository.saveAll(aggregated::iterator);

        return RepeatStatus.FINISHED;
    }

    private ParticipantPositionEntity mapToParticipantPositionEntity(final ParticipantCurrencyPairAmount position) {

        final CurrencyPairEntity currencyPair = position.getCurrencyPair();
        final LocalDate nextDate = tradeAndSettlementDateService.getNextTradeDate(businessDate, currencyPair);

        return participantPositionForPairMapper.toParticipantPosition(
            position,
            ParticipantPositionType.SOD,
            nextDate,
            tradeAndSettlementDateService.getValueDate(nextDate, currencyPair),
            dailySettlementPriceProvider.getDailySettlementPrices(businessDate).get(currencyPair)
        );
    }
}
