package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;
import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.matchingComparator;

import java.util.stream.Stream;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class ParticipantCodeOrderIdProvider extends OrderIdProvider {

    private final ParticipantRepository participantRepository;

    @Override
    public Stream<String> loadDataStream() {
        return participantRepository.findAllCodes().stream().sorted(matchingComparator(CLEARING_HOUSE_CODE::equals, false));
    }

}
