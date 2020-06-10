package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.ACTIVE;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.INACTIVE;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantStatus.SUSPENDED;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity_;
import com.ihsmarkit.tfx.core.domain.Participant;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class SpecificationFactory {

    public static PathSpecification<ParticipantEntity> participantPathSpecification() {
        return (root, query, cb) -> cb.and(
            root.get(ParticipantEntity_.status).in(ACTIVE, INACTIVE, SUSPENDED),
            cb.notEqual(root.get(ParticipantEntity_.code), Participant.CLEARING_HOUSE_CODE)
        );
    }

    @FunctionalInterface
    public interface PathSpecification<T> {

        @Nullable
        Predicate toPredicate(Path<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

        default Specification<T> toRootSpecification() {
            return this::toPredicate;
        }

    }

}
