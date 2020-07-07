package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        log.info("{} start waiting", TASKLET_LABEL);
        Thread.sleep(TimeUnit.MILLISECONDS.toMillis(15));
        jdbcTemplate.query(
            "  select " +
                " tradeentit0_.id as id1_54_0_," +
                "  legalentit1_.id as id1_39_1_," +
                "  participan2_.id as id1_47_2_, " +
                "  legalentit3_.id as id1_39_3_," +
                "  participan4_.id as id1_47_4_," +
                "  tradeentit0_.account_info as account_2_54_0_," +
                "  tradeentit0_.activity as activity3_54_0_," +
                "  tradeentit0_.base_ccy as base_ccy4_54_0_," +
                "  tradeentit0_.base_ccy_amt as base_ccy5_54_0_," +
                "  tradeentit0_.clearing_ref as clearing6_54_0_," +
                "  tradeentit0_.clearing_status as clearing7_54_0_," +
                "  tradeentit0_.clearing_tsp as clearing8_54_0_," +
                "  tradeentit0_.comments as comments9_54_0_," +
                "  tradeentit0_.counterparty_id as counter37_54_0_," +
                "  tradeentit0_.currency_pair_id as currenc38_54_0_," +
                "  tradeentit0_.direction as directi10_54_0_," +
                "  tradeentit0_.error_code as error_c11_54_0_," +
                "  tradeentit0_.execution_time as executi12_54_0_," +
                "  tradeentit0_.fix_session_id as fix_ses13_54_0_," +
                "  tradeentit0_.matched_trade_ref as matched14_54_0_," +
                "  tradeentit0_.matching_own_hash as matchin15_54_0_," +
                "  tradeentit0_.matching_ref as matchin16_54_0_," +
                "  tradeentit0_.matching_search_hash as matchin17_54_0_," +
                "  tradeentit0_.matching_status as matchin18_54_0_," +
                "  tradeentit0_.matching_tsp as matchin19_54_0_," +
                "  tradeentit0_.message_id as message20_54_0_," +
                "  tradeentit0_.obo_comment as obo_com21_54_0_," +
                "  tradeentit0_.old_version_id as old_ver39_54_0_," +
                "  tradeentit0_.originator_id as origina40_54_0_," +
                "  tradeentit0_.product_code as product22_54_0_," +
                "  tradeentit0_.sequence_id as sequenc23_54_0_," +
                "  tradeentit0_.source_system as source_24_54_0_," +
                "  tradeentit0_.spot_rate as spot_ra25_54_0_," +
                "  tradeentit0_.submission_tsp as submiss26_54_0_," +
                "  tradeentit0_.trade_date as trade_d27_54_0_," +
                "  tradeentit0_.trade_ref as trade_r28_54_0_," +
                "  tradeentit0_.transaction_type as transac29_54_0_," +
                "  tradeentit0_.user_name as user_na30_54_0_," +
                "  tradeentit0_.uti_prefix as uti_pre31_54_0_," +
                "  tradeentit0_.uti_trade_id as uti_tra32_54_0_," +
                "  tradeentit0_.value_ccy as value_c33_54_0_," +
                "  tradeentit0_.value_ccy_amt as value_c34_54_0_," +
                "  tradeentit0_.value_date as value_d35_54_0_," +
                "  tradeentit0_.version_tsp as version36_54_0_," +
                "  legalentit1_.clearing_role as clearing2_39_1_," +
                "  legalentit1_.code as code3_39_1_," +
                "  legalentit1_.lei as lei4_39_1_," +
                "  legalentit1_.name as name5_39_1_," +
                "  legalentit1_.participant_id as particip8_39_1_," +
                "  legalentit1_.status as status6_39_1_," +
                "  legalentit1_.swift as swift7_39_1_," +
                "  participan2_.code as code2_47_2_," +
                "  participan2_.matching_preference as matching3_47_2_," +
                "  participan2_.name as name4_47_2_," +
                "  participan2_.notification_email as notifica5_47_2_," +
                "  participan2_.status as status6_47_2_," +
                "  participan2_.type as type7_47_2_," +
                "  legalentit3_.clearing_role as clearing2_39_3_," +
                "  legalentit3_.code as code3_39_3_," +
                "  legalentit3_.lei as lei4_39_3_," +
                "  legalentit3_.name as name5_39_3_," +
                "  legalentit3_.participant_id as particip8_39_3_," +
                "  legalentit3_.status as status6_39_3_," +
                "  legalentit3_.swift as swift7_39_3_," +
                "  participan4_.code as code2_47_4_," +
                "  participan4_.matching_preference as matching3_47_4_," +
                "  participan4_.name as name4_47_4_," +
                "  participan4_.notification_email as notifica5_47_4_," +
                "  participan4_.status as status6_47_4_," +
                "  participan4_.type as type7_47_4_" +
                "  from" +
                "   trade tradeentit0_" +
                "  inner join" +
                "  legal_entity legalentit1_" +
                "  on tradeentit0_.originator_id=legalentit1_.id" +
                "  inner join" +
                "  participant participan2_" +
                "  on legalentit1_.participant_id=participan2_.id" +
                "  inner join" +
                "  legal_entity legalentit3_" +
                "  on tradeentit0_.counterparty_id=legalentit3_.id" +
                "  inner join" +
                "  participant participan4_" +
                "  on legalentit3_.participant_id=participan4_.id" +
                "  where" +
                "  tradeentit0_.trade_date= ?" +
                "  and tradeentit0_.transaction_type=2 ",
            rs -> {
                rs.last();
                log.info("result size: {}", rs.getRow());
                return null;
            },
            businessDate
        );

        log.info("{} loading rebalanced trades", TASKLET_LABEL);
        final List<TradeEntity> trades = tradeRepository.findAllBalanceByTradeDate(businessDate);

        log.info("{} publishing rebalance results for {} trades", TASKLET_LABEL, trades.size());
        publishingService.publishTrades(businessDate, trades);


        log.info("{} end", TASKLET_LABEL);
        return RepeatStatus.FINISHED;
    }
}
