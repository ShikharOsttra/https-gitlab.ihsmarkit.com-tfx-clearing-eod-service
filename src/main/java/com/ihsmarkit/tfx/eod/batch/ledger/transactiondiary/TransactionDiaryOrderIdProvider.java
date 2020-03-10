package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;

import lombok.RequiredArgsConstructor;

@StepScope
@Component
@RequiredArgsConstructor
public class TransactionDiaryOrderIdProvider {

    private static final int SUFFIX_SIZE = 10;
    private static final long DEFAULT_TRADE_SUFFIX = 1;

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    public long getOrderId(final String participantCode, final String productCode, final char suffix) {
        return getOrderId(participantCode, productCode, StringUtils.repeat(suffix, SUFFIX_SIZE));
    }

    public long getOrderId(final String participantCode, final String productCode, @Nullable final LocalDateTime clearingTsp) {
        String clearingTspStr = Long.toString(clearingTsp == null ? DEFAULT_TRADE_SUFFIX : clearingTsp.toEpochSecond(ZoneOffset.UTC));
        final int clearingTspStrLength = clearingTspStr.length();

        if (clearingTspStrLength > SUFFIX_SIZE) {
            clearingTspStr = clearingTspStr.substring(clearingTspStrLength - SUFFIX_SIZE);
        }

        if (clearingTspStrLength < SUFFIX_SIZE) {
            clearingTspStr = StringUtils.leftPad(clearingTspStr, SUFFIX_SIZE, '0');
        }

        return getOrderId(participantCode, productCode, clearingTspStr);
    }

    private long getOrderId(final String participantCode, final String productCode, final String suffix) {
        return OrderUtils.buildOrderId(
            participantCodeOrderIdProvider.get(participantCode),
            productCode,
            suffix
        );
    }

}
