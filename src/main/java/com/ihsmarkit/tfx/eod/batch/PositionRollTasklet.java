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
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@JobScope
@Slf4j
public class PositionRollTasklet implements Tasklet {

    private static final String TASKLET_LABEL = "[rollPositions]";

    private final ParticipantPositionRepository participantPositionRepository;

    private final EODCalculator eodCalculator;

    private final ParticipantCurrencyPairAmountMapper participantCurrencyPairAmountMapper;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        log.info("{} start", TASKLET_LABEL);

        log.info("{} loading net and rebalance positions", TASKLET_LABEL);
        final Stream<ParticipantPositionEntity> aggregatedSodPositions = eodCalculator
            .aggregatePositions(participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(businessDate))
            .map(this::mapToSodParticipantPositionEntity);

        log.info("{} persisting next date SOD positions", TASKLET_LABEL);
        participantPositionRepository.saveAll(aggregatedSodPositions::iterator);

        log.info("{} start", TASKLET_LABEL);
        return RepeatStatus.FINISHED;
    }

    private ParticipantPositionEntity mapToSodParticipantPositionEntity(final ParticipantCurrencyPairAmount position) {

        final CurrencyPairEntity currencyPair = position.getCurrencyPair();
        final LocalDate nextDate = tradeAndSettlementDateService.getNextTradeDate(businessDate, currencyPair);

        return participantCurrencyPairAmountMapper.toParticipantPosition(
            position,
            ParticipantPositionType.SOD,
            nextDate,
            businessDate,
            tradeAndSettlementDateService.getValueDate(nextDate, currencyPair),
            dailySettlementPriceService.getPrice(businessDate, currencyPair)
        );
    }
}
