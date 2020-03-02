package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity_;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantAndCurrencyPairQueryProvider;

@Service
@StepScope
public class MonthlyTradingVolumeQueryProvider extends ParticipantAndCurrencyPairQueryProvider {

    public MonthlyTradingVolumeQueryProvider(@Value("#{jobParameters['businessDate']}") final LocalDate businessDate) {
        super((root, cb) ->
            cb.between(
                root.get(ParticipantPositionEntity_.tradeDate),
                businessDate.with(firstDayOfMonth()),
                businessDate.with(lastDayOfMonth())
            )
        );
    }

}
