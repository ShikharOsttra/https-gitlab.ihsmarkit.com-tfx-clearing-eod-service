package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.AbstractTotalProcessor;
import com.ihsmarkit.tfx.eod.model.BuySellAmounts;
import com.ihsmarkit.tfx.eod.model.ParticipantAndCurrencyPair;
import com.ihsmarkit.tfx.eod.model.ledger.MonthlyTradingVolumeItem;

import lombok.RequiredArgsConstructor;

@StepScope
@Component
@RequiredArgsConstructor
public class MonthlyTradingVolumeTotalProcessor
    extends AbstractTotalProcessor<String, BuySellAmounts, ParticipantAndCurrencyPair, MonthlyTradingVolumeItem<BigDecimal>, MonthlyTradingVolumeItem<String>> {

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
        // TODO: to be added
        return List.of();
    }
}
