package com.ihsmarkit.tfx.eod.mtm;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RequiredArgsConstructor(staticName = "of")
@Getter
@Builder
public class TradeOrPositionEssentials {

    @NonNull
    private final CurrencyPairEntity currencyPair;

    @NonNull
    private final ParticipantEntity participant;

    @NonNull
    private final BigDecimal baseAmount;

    @NonNull
    private final BigDecimal spotRate;

}
