package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;

import java.io.Serializable;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TfxAndParticipantTotalProcessor<I,
    TK extends Serializable, TV extends TotalValue<TV>,
    PK extends ParticipantTotalKey<PK>, PV extends TotalValue<PV>>
    implements TotalProcessor<I> {

    private final MapTotalHolder<TK, TV> tfxTotal;
    private final MapTotalHolder<PK, PV> participantTotal;

    @Override
    public I process(final I item) {
        @Nullable
        final TK toTfxKey = toTfxKey(item);
        if (toTfxKey != null) {
            tfxTotal.contributeToTotals(toTfxKey, toTfxValue(item));
        }

        @Nullable
        final PK participantKey = toParticipantKey(item);
        if (participantKey != null) {
            final PV participantValue = toParticipantValue(item);
            participantTotal.contributeToTotals(participantKey, participantValue);
            participantTotal.contributeToTotals(participantKey.withParticipantCode(CLEARING_HOUSE_CODE), participantValue);
        }
        return item;
    }

    @Nullable
    protected abstract TK toTfxKey(I item);

    protected abstract TV toTfxValue(I item);

    @Nullable
    protected abstract PK toParticipantKey(I item);

    protected abstract PV toParticipantValue(I item);

}
