package com.ihsmarkit.tfx.eod.mtm;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import lombok.*;

import java.math.BigDecimal;

@RequiredArgsConstructor(staticName = "of")
@Getter
@ToString
public class TradeMTM {

    @NonNull
    private final ParticipantEntity participant;

    @NonNull
    private final CurrencyPairEntity currencyPair;

    @NonNull
    private final BigDecimal amount;
}
