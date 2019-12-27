package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType;

@Service
@JobScope
public class SODQueryProvider extends ParticipantPositionQueryProvider {

    public SODQueryProvider(@Value("#{jobParameters['businessDate']}") final LocalDate businessDate) {
        super(businessDate, ParticipantPositionType.SOD);
    }
}
