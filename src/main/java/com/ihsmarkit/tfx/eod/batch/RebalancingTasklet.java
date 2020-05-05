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
import com.ihsmarkit.tfx.core.domain.Amount;
import com.ihsmarkit.tfx.core.domain.CurrencyPair;
import com.ihsmarkit.tfx.core.domain.transaction.NewTransaction;
import com.ihsmarkit.tfx.core.domain.transaction.NewTransactionsRequest;
import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;
import com.ihsmarkit.tfx.eod.mapper.ParticipantCurrencyPairAmountMapper;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.ParticipantCurrencyPairAmount;
import com.ihsmarkit.tfx.eod.service.DailySettlementPriceService;
import com.ihsmarkit.tfx.eod.service.EODCalculator;
import com.ihsmarkit.tfx.eod.service.PositionRebalancePublishingService;
import com.ihsmarkit.tfx.eod.service.TradeAndSettlementDateService;
import com.ihsmarkit.tfx.eod.service.TransactionsSender;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;

@Service
@RequiredArgsConstructor
@JobScope
public class RebalancingTasklet implements Tasklet {

    private final ParticipantPositionRepository participantPositionRepository;

    private final TradeRepository tradeRepository;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final EODCalculator eodCalculator;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    private final ParticipantCurrencyPairAmountMapper participantCurrencyPairAmountMapper;

    private final PositionRebalancePublishingService publishingService;

    private final EODThresholdFutureValueRepository eodThresholdFutureValueRepository;

    private final TransactionsSender transactionsSender;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        final Stream<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllNetPositionsOfActiveLPByTradeDateFetchParticipant(businessDate);

        final Map<CurrencyPairEntity, Long> thresholds = eodThresholdFutureValueRepository.findByBusinessDate(businessDate).stream()
            .collect(Collectors.toMap(setting -> setting.getFxSpotProduct().getCurrencyPair(), EODThresholdFutureValueEntity::getValue));

        final Map<CurrencyPairEntity, List<BalanceTrade>> balanceTrades = eodCalculator.rebalanceLPPositions(positions, thresholds);

        final List<NewTransaction> newTransactions = EntryStream.of(balanceTrades)
            .flatMapKeyValue((currencyPair, tradesByCcy) ->
                EntryStream.of(
                    tradesByCcy.stream()
                        .collect(
                            Collectors.groupingBy(
                                BalanceTrade::getOriginator,
                                Collectors.groupingBy(
                                    BalanceTrade::getCounterparty,
                                    Streams.summingBigDecimal(BalanceTrade::getAmount)
                                )
                            )
                        )
                )
                    .flatMapKeyValue((originator, counterpartyAmounts) ->
                        EntryStream.of(counterpartyAmounts)
                            .mapKeyValue((counterparty, counterpartyAmount) ->
                                NewTransaction.builder()
                                    .tradeDate(businessDate)
                                    .tradeType(TransactionType.BALANCE)
                                    .buyerParticipantId(counterpartyAmount.signum() > 0 ? originator.getCode() : counterparty.getCode())
                                    .sellerParticipantId(counterpartyAmount.signum() > 0 ? counterparty.getCode() : originator.getCode())
                                    .currencyPair(CurrencyPair.of(currencyPair.getBaseCurrency(), currencyPair.getValueCurrency()))
                                    .spotRate(dailySettlementPriceService.getPrice(businessDate, currencyPair))
                                    .baseCurrencyAmount(Amount.of(counterpartyAmount.abs(), currencyPair.getBaseCurrency()))
                                    .build()
                            )
                    )
            ).toList();

        if (!newTransactions.isEmpty()) {
            transactionsSender.send(NewTransactionsRequest.builder()
                .transactions(newTransactions)
                .build()
            );
        }

        final Stream<ParticipantPositionEntity> rebalanceNetPositions = eodCalculator.netAll(
            EntryStream.of(balanceTrades)
                .flatMapKeyValue((currencyPair, ccyPairTrades) ->
                    ccyPairTrades.stream()
                        .flatMap(
                            trade -> Stream.of(
                                ParticipantCurrencyPairAmount.of(trade.getOriginator(), currencyPair, trade.getAmount()),
                                ParticipantCurrencyPairAmount.of(trade.getCounterparty(), currencyPair, trade.getAmount().negate())
                            )
                        )
                )
        )
            .map(trade -> participantCurrencyPairAmountMapper.toParticipantPosition(
                trade,
                ParticipantPositionType.REBALANCING,
                businessDate,
                businessDate,
                tradeAndSettlementDateService.getValueDate(businessDate, trade.getCurrencyPair()),
                dailySettlementPriceService.getPrice(businessDate, trade.getCurrencyPair())
            ));

        participantPositionRepository.saveAll(rebalanceNetPositions::iterator);

        final List<TradeEntity> trades = tradeRepository.findAllBalanceByTradeDate(businessDate);
        publishingService.publishTrades(businessDate, trades);

        return RepeatStatus.FINISHED;
    }
}
