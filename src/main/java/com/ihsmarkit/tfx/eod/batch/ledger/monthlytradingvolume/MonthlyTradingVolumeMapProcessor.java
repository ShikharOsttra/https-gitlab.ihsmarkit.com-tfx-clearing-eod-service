package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatYearMonth;
import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildOrderId;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeItem;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.domain.MonthlyTradingVolumeWriteItem;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MonthlyTradingVolumeMapProcessor implements ItemProcessor<MonthlyTradingVolumeItem, MonthlyTradingVolumeWriteItem> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final FXSpotProductService fxSpotProductService;

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @Override
    public MonthlyTradingVolumeWriteItem process(final MonthlyTradingVolumeItem item) {
        final ParticipantEntity participant = item.getParticipantAndCurrencyPair().getParticipant();

        return itemBuilder(participant.getCode(), item.getParticipantAndCurrencyPair().getCurrencyPair().getCode(), item.getBuySellAmounts())
            .participantName(participant.getName())
            .participantType(formatEnum(participant.getType()))
            .build();
    }

    public List<MonthlyTradingVolumeWriteItem> mapTfxTotal(final Map<String, BuySellAmounts> totals) {
        return EntryStream.of(totals)
            .mapKeyValue((currencyPair, total) ->
                itemBuilder(CLEARING_HOUSE_CODE, currencyPair, total)
                    .participantName(TFX_TOTAL)
                    .build()
            ).toList();
    }

    public List<MonthlyTradingVolumeWriteItem> mapParticipantTotal(final Map<MonthlyTradingVolumeParticipantTotalKey, BuySellAmounts> totals) {
        return EntryStream.of(totals)
            .mapKeyValue((key, buySellAmounts) ->
                MonthlyTradingVolumeWriteItem.builder()
                    .businessDate(businessDate.with(firstDayOfMonth()))
                    .participantCode(key.getParticipantCode())
                    .currencyPairCode(TOTAL)
                    .sellTradingVolumeInUnit(buySellAmounts.getSell().toPlainString())
                    .buyTradingVolumeInUnit(buySellAmounts.getBuy().toPlainString())
                    .orderId(Long.MAX_VALUE)
                    .recordType(PARTICIPANT_TOTAL_RECORD_TYPE)
                    .build()
            ).toList();
    }

    private MonthlyTradingVolumeWriteItem.MonthlyTradingVolumeWriteItemBuilder itemBuilder(
        final String participantCode,
        final String currencyPairCode,
        final BuySellAmounts buySellAmounts
    ) {
        final var productNumber = fxSpotProductService.getFxSpotProduct(currencyPairCode).getProductNumber();

        return MonthlyTradingVolumeWriteItem.builder()
            .businessDate(businessDate.with(firstDayOfMonth()))
            .tradeDate(formatYearMonth(businessDate))
            .recordDate(formatDateTime(recordDate))
            .participantCode(participantCode)
            .currencyPairCode(currencyPairCode)
            .currencyPairNumber(productNumber)
            .sellTradingVolumeInUnit(buySellAmounts.getSell().toPlainString())
            .buyTradingVolumeInUnit(buySellAmounts.getBuy().toPlainString())
            .orderId(getOrderId(participantCode, productNumber))
            .recordType(ITEM_RECORD_TYPE);
    }

    private long getOrderId(final String participantCode, final String productNumber) {
        return buildOrderId(
            participantCodeOrderIdProvider.get(participantCode),
            productNumber
        );
    }
}
