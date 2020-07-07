package com.ihsmarkit.tfx.eod.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.EodCashSettlementMappingService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class MarkToMarketTradesTasklet implements Tasklet {

    private static final String TASKLET_LABEL = "[mtmTrades]";

    private final TradeRepository tradeRepository;

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final ParticipantPositionRepository participantPositionRepository;

    private final EODCalculator eodCalculator;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final JPYRateService jpyRateService;

    private final EodCashSettlementMappingService eodCashSettlementMappingService;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        log.info("{} start", TASKLET_LABEL);

        final Function<CurrencyPairEntity, BigDecimal> dspResolver = ccy -> dailySettlementPriceService.getPrice(businessDate, ccy);
        final Function<String, BigDecimal> jpyRatesResolver = ccy -> jpyRateService.getJpyRate(businessDate, ccy);

        log.info("{} loading novated trades", TASKLET_LABEL);
        final Stream<TradeEntity> novatedTrades = tradeRepository.findAllNovatedForTradeDate(businessDate);

        log.info("{} calculating initial mtm", TASKLET_LABEL);
        final Stream<EodProductCashSettlementEntity> initial =
            eodCalculator.calculateAndAggregateInitialMtm(novatedTrades, dspResolver, jpyRatesResolver)
                .map(eodCashSettlementMappingService::mapInitialMtm);

        log.info("{} loading SOD positions", TASKLET_LABEL);
        final Stream<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(ParticipantPositionType.SOD, businessDate);

        log.info("{} calculating daily mtm", TASKLET_LABEL);
        final Stream<EodProductCashSettlementEntity> daily =
            eodCalculator.calculateAndAggregateDailyMtm(positions, dspResolver, jpyRatesResolver)
                .map(eodCashSettlementMappingService::mapDailyMtm);

        log.info("{} persisting product cash settlements", TASKLET_LABEL);
        eodProductCashSettlementRepository.saveAll(Stream.concat(initial, daily)::iterator);

        log.info("{} end", TASKLET_LABEL);
        return RepeatStatus.FINISHED;
    }

}
