package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.common.streams.Streams;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.EODThresholdFutureValueEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.EODThresholdFutureValueRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.eod.mapper.BalanceTradeMapper;
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.PositionRebalancePublishingService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class RebalancingTasklet implements Tasklet {

    private final ParticipantPositionRepository participantPositionRepository;

    private final TradeRepository tradeRepositiory;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final EODCalculator eodCalculator;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    private final BalanceTradeMapper balanceTradeMapper;

    private final ParticipantCurrencyPairAmountMapper participantCurrencyPairAmountMapper;

    private final PositionRebalancePublishingService publishingService;

    private final EODThresholdFutureValueRepository eodThresholdFutureValueRepository;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Stream<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllNetPositionsOfActiveLPByTradeDateFetchParticipant(businessDate);

        final Map<CurrencyPairEntity, Long> thresholds = eodThresholdFutureValueRepository.findByBusinessDate(businessDate).stream()
            .collect(Collectors.toMap(setting -> setting.getFxSpotProduct().getCurrencyPair(), EODThresholdFutureValueEntity::getValue));

        final Map<CurrencyPairEntity, List<BalanceTrade>> balanceTrades = eodCalculator.rebalanceLPPositions(positions, thresholds);

        final List<TradeEntity> trades = balanceTrades.entrySet().stream()
            .flatMap(
                tradesByCcy -> tradesByCcy.getValue().stream()
                    .collect(
                        Collectors.groupingBy(
                            BalanceTrade::getOriginator,
                            Collectors.groupingBy(
                                BalanceTrade::getCounterparty,
                                Streams.summingBigDecimal(BalanceTrade::getAmount)
                            )
                        )
                    ).entrySet().stream()
                    .flatMap(
                        byOriginator -> byOriginator.getValue().entrySet().stream()
                            .flatMap(byCounterpart ->
                                Stream.of(new BalanceTrade(byOriginator.getKey(), byCounterpart.getKey(), byCounterpart.getValue()),
                                    new BalanceTrade(byCounterpart.getKey(), byOriginator.getKey(), byCounterpart.getValue().negate())))
                    ).map(
                        trade -> balanceTradeMapper.toTrade(
                            trade,
                            businessDate,
                            tradeAndSettlementDateService.getValueDate(businessDate, tradesByCcy.getKey()),
                            tradesByCcy.getKey(),
                            dailySettlementPriceService.getPrice(businessDate, tradesByCcy.getKey())
                        )
                    )
                )
            .collect(Collectors.toList());

        tradeRepositiory.saveAll(trades);

        final Stream<ParticipantPositionEntity> rebalanceNetPositions = eodCalculator.netAll(
            balanceTrades.entrySet().stream()
                .flatMap(
                    tradesByCcy -> tradesByCcy.getValue().stream()
                        .flatMap(
                            trade -> Stream.of(
                                ParticipantCurrencyPairAmount.of(trade.getOriginator(), tradesByCcy.getKey(), trade.getAmount()),
                                ParticipantCurrencyPairAmount.of(trade.getCounterparty(), tradesByCcy.getKey(), trade.getAmount().negate())
                            )
                        )
                )
        ).map(trade -> participantCurrencyPairAmountMapper.toParticipantPosition(
            trade,
            ParticipantPositionType.REBALANCING,
            businessDate,
            tradeAndSettlementDateService.getValueDate(businessDate, trade.getCurrencyPair()),
            dailySettlementPriceService.getPrice(businessDate, trade.getCurrencyPair())
        ));

        participantPositionRepository.saveAll(rebalanceNetPositions::iterator);
        publishingService.publishTrades(businessDate, trades);

        return RepeatStatus.FINISHED;
    }
}
