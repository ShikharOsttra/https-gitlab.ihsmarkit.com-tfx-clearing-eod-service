package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;

@Service
@RequiredArgsConstructor
@JobScope
@Slf4j
public class RebalancingTasklet implements Tasklet {

    private static final String TASKLET_LABEL = "[rebalancePositions]";

    private final ParticipantPositionRepository participantPositionRepository;

    private final TradeRepository tradeRepository;

    private final DailySettlementPriceService dailySettlementPriceService;

    private final EODCalculator eodCalculator;

    private final TradeAndSettlementDateService tradeAndSettlementDateService;

    private final ParticipantCurrencyPairAmountMapper participantCurrencyPairAmountMapper;

    private final PositionRebalancePublishingService publishingService;

    private final EODThresholdFutureValueRepository eodThresholdFutureValueRepository;

    private final TransactionsSender transactionsSender;

    private final JdbcTemplate jdbcTemplate;

    private final EntityManager entityManager;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    @SneakyThrows
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        log.info("{} start", TASKLET_LABEL);
        log.info("{} loading positions", TASKLET_LABEL);
        final Stream<ParticipantPositionEntity> positions =
            participantPositionRepository.findAllNetPositionsOfActiveLPByTradeDateFetchParticipant(businessDate);

        log.info("{} loading thresholds", TASKLET_LABEL);
        final Map<CurrencyPairEntity, Long> thresholds = eodThresholdFutureValueRepository.findByBusinessDate(businessDate).stream()
            .collect(Collectors.toMap(setting -> setting.getFxSpotProduct().getCurrencyPair(), EODThresholdFutureValueEntity::getValue));

        log.info("{} calculating rebalance positions", TASKLET_LABEL);
        final Map<CurrencyPairEntity, List<BalanceTrade>> balanceTrades = eodCalculator.rebalanceLPPositions(positions, thresholds);

        log.info("{} calculating rebalance positions", TASKLET_LABEL);
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
            log.info("{} publishing {} rebalance transactions", TASKLET_LABEL, newTransactions.size());
            transactionsSender.send(NewTransactionsRequest.builder()
                .transactions(newTransactions)
                .build()
            );
        }

        log.info("{} calculating net positions after rebalance", TASKLET_LABEL);
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

        log.info("{} persisting rebalance net positions", TASKLET_LABEL);
        participantPositionRepository.saveAll(rebalanceNetPositions::iterator);

        entityManager.unwrap(Session.class).doWork(connection -> log.info("isolation level: {}", Environment.isolationLevelToString(connection.getTransactionIsolation())));

        log.info("{} loading rebalanced trades", TASKLET_LABEL);
        final List<TradeEntity> trades = tradeRepository.findAllBalanceByTradeDate(businessDate);

        log.info("{} publishing rebalance results for {} trades", TASKLET_LABEL, trades.size());
        publishingService.publishTrades(businessDate, trades);

        log.info("{} end", TASKLET_LABEL);
        return RepeatStatus.FINISHED;
    }
}
