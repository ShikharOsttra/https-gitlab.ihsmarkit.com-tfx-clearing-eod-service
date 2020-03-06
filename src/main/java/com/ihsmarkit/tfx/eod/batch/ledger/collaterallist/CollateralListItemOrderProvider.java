package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildOrderId;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralListItemOrderProvider {

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    public Long getOrderId(final CollateralBalanceEntity balance, final int recordType) {
        return getOrderId(
            balance.getParticipant().getCode(),
            balance.getPurpose().getValue(),
            balance.getProduct().getType().getValue(),
            recordType
        );
    }

    public Long getOrderId(final CollateralListItemTotalKey collateralListItemTotalKey, final int recordType) {
        return getOrderId(
            collateralListItemTotalKey.getParticipantCode(),
            collateralListItemTotalKey.getCollateralPurposeType(),
            collateralListItemTotalKey.getCollateralTypeNo(),
            recordType
        );
    }

    private Long getOrderId(final String participantCode, final Object collateralPurposeType, final Object collateralType, final int recordType) {
        return buildOrderId(
            participantCodeOrderIdProvider.get(participantCode),
            collateralPurposeType,
            collateralType,
            recordType
        );
    }
}
