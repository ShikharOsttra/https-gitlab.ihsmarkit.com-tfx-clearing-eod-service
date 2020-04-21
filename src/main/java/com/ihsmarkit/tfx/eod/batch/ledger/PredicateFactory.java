package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.ACTIVE;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.INACTIVE;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.SUSPENDED;

import java.util.function.BiFunction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity_;
import com.ihsmarkit.tfx.core.domain.Participant;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class PredicateFactory {

    public static BiFunction<CriteriaBuilder, Root<ParticipantEntity>, Predicate[]> participantPredicate() {
        return (cb, root) -> new Predicate[] {
            root.get(ParticipantEntity_.status).in(ACTIVE, INACTIVE, SUSPENDED),
            cb.notEqual(root.get(ParticipantEntity_.code), Participant.CLEARING_HOUSE_CODE)
        };
    }

}
