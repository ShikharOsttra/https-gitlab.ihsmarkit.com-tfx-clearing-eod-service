package com.ihsmarkit.tfx.eod.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
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
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.EODCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class PositionRollTasklet implements Tasklet {

    private final ParticipantPositionRepository participantPositionRepository;

    private final EODCalculator eodCalculator;

    private final ParticipantPositionForPairMapper participantPositionForPairMapper;


    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

        final Map<CurrencyPairEntity, BigDecimal> dsp = dailySettlementPriceProvider.getDailySettlementPrices(businessDate);

        Stream<ParticipantPositionEntity> positions = participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(businessDate);
        Stream<ParticipantPositionEntity> aggregated = eodCalculator.aggregatePositions(positions)
            .map(
                position -> participantPositionForPairMapper.toParticipantPosition(
                    position,
                    ParticipantPositionType.SOD,
                    businessDate.plusDays(1),
                    businessDate.plusDays(4), //FIXME: settlement date by ccy?
                    dsp.get(position.getCurrencyPair())
                )
            );

        participantPositionRepository.saveAll(aggregated::iterator);

        return RepeatStatus.FINISHED;
    }
}
