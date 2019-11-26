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
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantPositionForPairMapper;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.SettlementDateProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class NettingTasklet implements Tasklet {

    private final TradeRepository tradeRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    private final EODCalculator eodCalculator;

    private final SettlementDateProvider settlementDateProvider;

    private final ParticipantPositionForPairMapper participantPositionForPairMapper;

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

        final LocalDate settlementDate = settlementDateProvider.getSettlementDateFor(businessDate);

        final Map<CurrencyPairEntity, BigDecimal> dsp = dailySettlementPriceProvider.getDailySettlementPrices(businessDate);

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate)
            .stream();

        final Stream<TradeOrPositionEssentials> tradesToNet = Stream.concat(
            novatedTrades.map(tradeOrPositionMapper::convertTrade),
            positions.map(tradeOrPositionMapper::convertPosition)
        );

        final Stream<ParticipantPositionEntity> netted = eodCalculator.netAllTtrades(tradesToNet)
            .map(trade -> participantPositionForPairMapper.toParticipantPosition(
                trade,
                ParticipantPositionType.NET,
                businessDate,
                settlementDate, //FIXME: settlement date by ccy?
                dsp.get(trade.getCurrencyPair())
            ));

        participantPositionRepository.saveAll(netted::iterator);

        return RepeatStatus.FINISHED;
    }

}
