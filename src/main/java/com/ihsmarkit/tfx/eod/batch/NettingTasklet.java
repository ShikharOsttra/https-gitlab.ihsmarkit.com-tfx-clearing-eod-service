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

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class NettingTasklet implements Tasklet {

    private final TradeRepository tradeRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final EODCalculator eodCalculator;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    private final ParticipantCurrencyPairAmountMapper participantCurrencyPairAmountMapper;

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate)
            .stream();

        final Stream<TradeOrPositionEssentials> tradesToNet = Stream.concat(
            novatedTrades.map(tradeOrPositionMapper::convertTrade),
            positions.map(tradeOrPositionMapper::convertPosition)
        );

        final Stream<ParticipantPositionEntity> netted = eodCalculator.netAllByBuySell(tradesToNet)
            .map(position -> participantCurrencyPairAmountMapper.toParticipantPosition(
                position,
                businessDate,
                tradeAndSettlementDateService.getValueDate(businessDate, position.getCurrencyPair()),
                dailySettlementPriceService.getPrice(businessDate, position.getCurrencyPair())
            ));

        participantPositionRepository.saveAll(netted::iterator);

        return RepeatStatus.FINISHED;
    }

}
