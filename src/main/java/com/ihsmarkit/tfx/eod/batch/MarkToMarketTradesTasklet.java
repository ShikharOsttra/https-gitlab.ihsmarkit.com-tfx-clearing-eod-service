package com.ihsmarkit.tfx.eod.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Function;
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
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantPositionForPairMapper;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.JPYRatesService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@JobScope
public class MarkToMarketTradesTasklet implements Tasklet {

    private final TradeRepository tradeRepository;

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final EODCalculator eodCalculator;

    private final ParticipantPositionForPairMapper mtmMapper;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final JPYRatesService jpyRatesService;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Function<CurrencyPairEntity, BigDecimal> dspResolver = ccy -> dailySettlementPriceService.getPrice(businessDate, ccy);
        final Function<String, BigDecimal> jpyRates = ccy -> jpyRatesService.getJpyRate(businessDate, ccy);

        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);
        final Stream<EodProductCashSettlementEntity> initial = eodCalculator.calculateAndAggregateInitialMtm(novatedTrades, dspResolver, jpyRates)
            .map(
                mtm -> mtmMapper.toEodProductCashSettlement(
                    mtm,
                    businessDate,
                    tradeAndSettlementDateService.getValueDate(businessDate, mtm.getCurrencyPair()),
                    EodProductCashSettlementType.INITIAL_MTM
                )
            );

        final Collection<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate);

        final Stream<EodProductCashSettlementEntity> daily = eodCalculator.calculateAndAggregateDailyMtm(positions, dspResolver, jpyRates)
            .map(
                mtm -> mtmMapper.toEodProductCashSettlement(
                    mtm,
                    businessDate,
                    tradeAndSettlementDateService.getValueDate(businessDate, mtm.getCurrencyPair()),
                    EodProductCashSettlementType.DAILY_MTM
                )
            );

        eodProductCashSettlementRepository.saveAll(Stream.concat(initial, daily)::iterator);

        return RepeatStatus.FINISHED;
    }

}
