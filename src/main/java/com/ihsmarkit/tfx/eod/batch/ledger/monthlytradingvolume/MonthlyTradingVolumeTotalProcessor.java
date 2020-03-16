package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.PARTICIPANT_TOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.AbstractTotalProcessor;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.MonthlyTradingVolumeItem;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;

@StepScope
@Component
@RequiredArgsConstructor
public class MonthlyTradingVolumeTotalProcessor
    extends AbstractTotalProcessor<String, BuySellAmounts, ParticipantAndCurrencyPair, MonthlyTradingVolumeItem<BigDecimal>, MonthlyTradingVolumeItem<String>> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    protected String toTotalKey(final MonthlyTradingVolumeItem<BigDecimal> monthlyTradingVolumeItem) {
        return monthlyTradingVolumeItem.getParticipantCode();
    }

    @Override
    protected BuySellAmounts toTotalValue(final MonthlyTradingVolumeItem<BigDecimal> monthlyTradingVolumeItem) {
        return BuySellAmounts.of(monthlyTradingVolumeItem.getBuyTradingVolumeInUnit(), monthlyTradingVolumeItem.getSellTradingVolumeInUnit());
    }

    @Override
    protected BuySellAmounts merge(final BuySellAmounts prev, final BuySellAmounts stepContribution) {
        return prev.add(stepContribution);
    }

    @Override
    protected List<MonthlyTradingVolumeItem<String>> extractTotals(final Map<String, BuySellAmounts> totals) {
        final BuySellAmounts totalOfTotals = totals.values().stream()
            .reduce(BuySellAmounts.empty(), BuySellAmounts::add);

        return EntryStream.of(totals)
            .mapKeyValue((participantCode, buySellAmounts) ->
                MonthlyTradingVolumeItem.<String>builder()
                    .businessDate(businessDate.with(firstDayOfMonth()))
                    .participantCode(participantCode)
                    .currencyPairCode(TOTAL)

                    .sellTradingVolumeInUnit(buySellAmounts.getSell().toString())
                    .buyTradingVolumeInUnit(buySellAmounts.getBuy().toString())

                    .orderId(Long.MAX_VALUE)
                    .recordType(PARTICIPANT_TOTAL_RECORD_TYPE)
                    .build()
            )
            .append(
                MonthlyTradingVolumeItem.<String>builder()
                    .businessDate(businessDate.with(firstDayOfMonth()))
                    .currencyPairCode(TOTAL)

                    .sellTradingVolumeInUnit(totalOfTotals.getSell().toString())
                    .buyTradingVolumeInUnit(totalOfTotals.getBuy().toString())

                    .orderId(Long.MAX_VALUE)
                    .recordType(TOTAL_RECORD_TYPE)
                    .build()
            )
            .toList();
    }
}
