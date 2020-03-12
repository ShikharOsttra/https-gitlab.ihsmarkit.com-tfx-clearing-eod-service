package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildOrderId;

import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.LogCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;
import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralListItemOrderProvider {

    //max size of issuer bank sub type and security codes
    private static final int PRODUCT_SPECIFIC_CODE_SIZE = 10;

    private final ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    public Long getOrderId(final CollateralBalanceEntity balance, final int recordType) {
        return getOrderId(
            balance.getParticipant().getCode(),
            balance.getPurpose().getValue(),
            recordType,
            balance.getProduct().getType().getValue(),
            getProductSpecificCode(balance.getProduct())
        );
    }

    private long getProductSpecificCode(final CollateralProductEntity product) {
        if (product instanceof SecurityCollateralProductEntity) {
            return Long.parseLong(((SecurityCollateralProductEntity) product).getSecurityCode());
        }

        if (product.getType() == CollateralProductType.LOG) {
            return ((LogCollateralProductEntity) product).getIssuer().getSubType();
        }

        return 0;
    }

    public Long getOrderId(final CollateralListItemTotalKey collateralListItemTotalKey, final int recordType) {
        return getOrderId(
            collateralListItemTotalKey.getParticipantCode(),
            collateralListItemTotalKey.getCollateralPurposeType(),
            recordType,
            0,
            0
        );
    }

    private Long getOrderId(final String participantCode, final Object collateralPurposeType, final int recordType, final int collateralType,
        final long productSpecificCode) {
        return buildOrderId(
            participantCodeOrderIdProvider.get(participantCode),
            collateralPurposeType,
            recordType,
            collateralType,
            StringUtils.leftPad(Long.toString(productSpecificCode), PRODUCT_SPECIFIC_CODE_SIZE, '0')
        );
    }
}
