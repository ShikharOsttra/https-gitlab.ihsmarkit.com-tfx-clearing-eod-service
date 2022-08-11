package com.ihsmarkit.tfx.eod.batch.ledger.rebalance;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalRoundTo1Jpy;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalStripZero;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildOrderId;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Tables;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceItem;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceParticipantTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceTfxTotal;
import com.ihsmarkit.tfx.eod.batch.ledger.rebalance.domain.RebalanceWriteItem;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@StepScope
@Slf4j
public class RebalanceMapProcessor implements ItemProcessor<RebalanceItem, RebalanceWriteItem> {

    private static final String SETTLEMENT_DATE_TOTAL = "Settlement Date Total";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final FXSpotProductService fxSpotProductService;
    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @Override
    public RebalanceWriteItem process(final RebalanceItem openPositionsItem) {
        final ParticipantAndCurrencyPair participantAndCurrencyPair = openPositionsItem.getParticipantAndCurrencyPair();
        final String productNumber = fxSpotProductService.getFxSpotProduct(participantAndCurrencyPair.getCurrencyPair()).getProductNumber();

        return RebalanceWriteItem.builder()
            .businessDate(businessDate)
            .participantCode(participantAndCurrencyPair.getParticipant().getCode())
            .participantName(participantAndCurrencyPair.getParticipant().getName())
            .participantType(formatEnum(participantAndCurrencyPair.getParticipant().getType()))
            .currencyCode(participantAndCurrencyPair.getCurrencyPair().getCode())
            .currencyNo(productNumber)
            .tradeDate(formatDate(businessDate))
            .shortPositionPreviousDay(formatBigDecimalStripZero(openPositionsItem.getShortPositionPreviousDay()))
            .longPositionPreviousDay(formatBigDecimalStripZero(openPositionsItem.getLongPositionPreviousDay()))
            .buyTradingAmount(formatBigDecimalStripZero(openPositionsItem.getBuyTradingAmount()))
            .sellTradingAmount(formatBigDecimalStripZero(openPositionsItem.getSellTradingAmount()))
            .shortPosition(formatBigDecimalStripZero(openPositionsItem.getShortPosition()))
            .longPosition(formatBigDecimalStripZero(openPositionsItem.getLongPosition()))
            .initialMtmAmount(formatBigDecimalRoundTo1Jpy(openPositionsItem.getInitialMtmAmount()))
            .dailyMtmAmount(formatBigDecimalRoundTo1Jpy(openPositionsItem.getDailyMtmAmount()))
            .swapPoint(formatBigDecimalRoundTo1Jpy(openPositionsItem.getSwapPoint()))
            .totalVariationMargin(formatBigDecimalRoundTo1Jpy(openPositionsItem.getTotalVariationMargin()))
            .settlementDate(formatDate(openPositionsItem.getSettlementDate()))
            .recordDate(formatDateTime(recordDate))
            .recordType(ITEM_RECORD_TYPE)
            .orderId(getOrderId(participantAndCurrencyPair.getParticipant().getCode(), productNumber))
            .build();
    }

    private long getOrderId(final String participantCode, final String productNumber) {
        return buildOrderId(
            participantCodeOrderIdProvider.get(participantCode),
            productNumber
        );
    }

    public List<RebalanceWriteItem> mapToTfxTotal(final Map<String, RebalanceTfxTotal> tfxTotals) {
        return EntryStream.of(tfxTotals).mapKeyValue((currencyPairCode, total) ->
            RebalanceWriteItem.builder()
                .businessDate(businessDate)
                .participantCode(CLEARING_HOUSE_CODE)
                .participantName(TFX_TOTAL)
                .currencyCode(currencyPairCode)
                .tradeDate(formatDate(businessDate))
                .shortPositionPreviousDay(formatBigDecimalStripZero(total.getShortPositionPreviousDay()))
                .longPositionPreviousDay(formatBigDecimalStripZero(total.getLongPositionPreviousDay()))
                .buyTradingAmount(formatBigDecimalStripZero(total.getBuyTradingAmount()))
                .sellTradingAmount(formatBigDecimalStripZero(total.getSellTradingAmount()))
                .shortPosition(formatBigDecimalStripZero(total.getShortPosition()))
                .longPosition(formatBigDecimalStripZero(total.getLongPosition()))
                .initialMtmAmount(formatBigDecimalRoundTo1Jpy(total.getInitialMtmAmount()))
                .dailyMtmAmount(formatBigDecimalRoundTo1Jpy(total.getDailyMtmAmount()))
                .swapPoint(formatBigDecimalRoundTo1Jpy(total.getSwapPoint()))
                .totalVariationMargin(formatBigDecimalRoundTo1Jpy(total.getTotalVariationMargin()))
                .settlementDate(formatDate(total.getSettlementDate()))
                .recordDate(formatDateTime(recordDate))
                .recordType(ITEM_RECORD_TYPE)
                .orderId(getOrderId(CLEARING_HOUSE_CODE, fxSpotProductService.getFxSpotProduct(currencyPairCode).getProductNumber()))
                .build()
        ).collect(Collectors.toList());
    }

    public List<RebalanceWriteItem> mapToParticipantTotal(final Map<RebalanceParticipantTotalKey, RebalanceParticipantTotal> participantTotals) {
        return groupParticipantTotalsByCode(participantTotals).flatMapKeyValue((participantCode, totals) -> {

            final RebalanceParticipantTotal totalOfSettlementDates = totals.values().stream()
                .reduce(RebalanceParticipantTotal.ZERO, RebalanceParticipantTotal::add);

            final List<LocalDate> settlementDates = totals.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

            return Stream.concat(
                Stream.of(
                    mapToParticipantTotal(participantCode, totalOfSettlementDates, totals.size(), TOTAL, null)
                ),

                EntryStream.of(settlementDates)
                    .mapKeyValue((index, settlementDate) ->
                        mapToParticipantTotal(
                            participantCode,
                            totals.get(settlementDate),
                            totals.size() - 1 - index,
                            SETTLEMENT_DATE_TOTAL,
                            settlementDate
                        ))
            );
        }).collect(Collectors.toList());
    }

    private RebalanceWriteItem mapToParticipantTotal(
        final String participantCode,
        final RebalanceParticipantTotal total,
        final int orderOffset,
        final String totalName,
        @Nullable final LocalDate settlementDate
    ) {
        return RebalanceWriteItem.builder()
            .businessDate(businessDate)
            .participantCode(participantCode)
            .currencyCode(totalName)
            .initialMtmAmount(formatBigDecimalRoundTo1Jpy(total.getInitialMtmAmount()))
            .dailyMtmAmount(formatBigDecimalRoundTo1Jpy(total.getDailyMtmAmount()))
            .swapPoint(formatBigDecimalRoundTo1Jpy(total.getSwapPoint()))
            .totalVariationMargin(formatBigDecimalRoundTo1Jpy(total.getTotalVariationMargin()))
            .settlementDate(formatDate(settlementDate))
            .recordType(PARTICIPANT_TOTAL_RECORD_TYPE)
            .orderId(Long.MAX_VALUE - orderOffset)
            .build();
    }

    private static EntryStream<String, Map<LocalDate, RebalanceParticipantTotal>> groupParticipantTotalsByCode(
        final Map<RebalanceParticipantTotalKey, RebalanceParticipantTotal> participantTotals
    ) {
        return EntryStream.of(
            EntryStream.of(participantTotals)
                .collect(
                    Tables.toTable(
                        entry -> entry.getKey().getParticipantCode(),
                        entry -> entry.getKey().getSettlementDate(),
                        Map.Entry::getValue,
                        HashBasedTable::create
                    )
                ).rowMap()
        );
    }

}
