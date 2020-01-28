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
import com.ihsmarkit.tfx.core.dl.entity.eod.EodProductCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodProductCashSettlementRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.EodCashSettlementMappingService;
import com.ihsmarkit.tfx.eod.service.JPYRateService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class SwapPnLTasklet implements Tasklet {

    private final EodProductCashSettlementRepository eodProductCashSettlementRepository;

    private final EODCalculator eodCalculator;

    private final CurrencyPairSwapPointService currencyPairSwapPointService;

    private final JPYRateService jpyRateService;

    private final EodCashSettlementMappingService eodCashSettlementMappingService;

    private final ParticipantPositionRepository participantPositionRepository;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Function<CurrencyPairEntity, BigDecimal> swapPointResolver = ccy -> currencyPairSwapPointService.getSwapPoint(businessDate, ccy);
        final Function<String, BigDecimal> jpyRatesResolver = ccy -> jpyRateService.getJpyRate(businessDate, ccy);

        final Stream<EodProductCashSettlementEntity> eodProductCashSettlementEntityStream = eodCalculator
            .aggregatePositions(participantPositionRepository.findAllNetAndRebalancingPositionsByTradeDate(businessDate))
            .map(position -> eodCalculator.calculateSwapPoint(position, swapPointResolver, jpyRatesResolver))
            .map(eodCashSettlementMappingService::mapSwapPnL);

        eodProductCashSettlementRepository.saveAll(eodProductCashSettlementEntityStream::iterator);

        return RepeatStatus.FINISHED;
    }

}
