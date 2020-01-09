package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity_;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantAndCurrencyPairQueryProvider;

@Service
@StepScope
public class OpenPositionsQueryProvider extends ParticipantAndCurrencyPairQueryProvider {

    public OpenPositionsQueryProvider(@Value("#{jobParameters['businessDate']}") final LocalDate businessDate) {
        super((root, cb) -> cb.equal(root.get(ParticipantPositionEntity_.tradeDate), businessDate));
    }

}
