package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;

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
import com.ihsmarkit.tfx.eod.mapper.ParticipantPositionForPairMapper;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceProvider;
import com.ihsmarkit.tfx.eod.service.NetCalculator;
import com.ihsmarkit.tfx.eod.service.SettlementDateProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class NettingTasklet implements Tasklet {

    private final TradeRepository tradeRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final DailySettlementPriceProvider dailySettlementPriceProvider;

    private final NetCalculator netCalculator;

    private final SettlementDateProvider settlementDateProvider;

    private final ParticipantPositionForPairMapper mapper;

    @Value("#{jobParameters['businessDate']}")
    private final String businessDateStr;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        final LocalDate businessDate = LocalDate.parse(businessDateStr, BUSINESS_DATE_FMT);
        final LocalDate settlementDate = settlementDateProvider.getSettlementDateFor(businessDate);

        final Map<CurrencyPairEntity, BigDecimal> dsp = dailySettlementPriceProvider.getDailySettlementPrices(businessDate);

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<ParticipantPositionEntity> netted = netCalculator.netAllTtrades(novatedTrades)
            .map(trade -> mapper.toParticipantPosition(
                trade,
                businessDate,
                settlementDate,
                dsp.get(trade.getCurrencyPair())
            ));

        participantPositionRepository.saveAll(netted::iterator);

        return RepeatStatus.FINISHED;
    }

}
