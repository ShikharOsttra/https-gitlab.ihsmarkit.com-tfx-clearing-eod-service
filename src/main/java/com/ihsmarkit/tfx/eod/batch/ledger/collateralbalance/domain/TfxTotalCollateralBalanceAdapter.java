package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance.domain;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.data.util.Pair;

import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.core.domain.type.EodCashSettlementDateType;
import com.ihsmarkit.tfx.core.domain.type.EodProductCashSettlementType;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;

import lombok.Value;
import lombok.experimental.Delegate;


@Value(staticConstructor = "of")
public class TfxTotalCollateralBalanceAdapter implements CollateralBalanceAdapter {

    @Delegate
    private final CollateralBalanceTotal total;

    @Override
    public String getParticipantCode() {
        return CLEARING_HOUSE_CODE;
    }

    @Override
    public String getParticipantName() {
        return TFX_TOTAL;
    }

    @Nullable
    @Override
    public ParticipantType getParticipantType() {
        return null;
    }

    @Override
    public boolean isShowLgBalance(final CollateralPurpose collateralPurpose) {
        return collateralPurpose == CollateralPurpose.MARGIN;
    }

    @Nullable
    @Override
    public BigDecimal getCashSettlement(final EodProductCashSettlementType settlementType, final EodCashSettlementDateType dateType) {
        return total.getCashSettlements().get(CashSettlementTotalKey.of(settlementType, dateType));
    }

    @Override
    public Optional<Pair<LocalDate, BigDecimal>> getNextClearingDeposit() {
        if (total.getNextClearingDepositRequiredAmount() == null || total.getNextClearingDepositApplicableDate() == null) {
            return Optional.empty();
        }
        return Optional.of(Pair.of(total.getNextClearingDepositApplicableDate(), total.getNextClearingDepositRequiredAmount()));
    }


}
