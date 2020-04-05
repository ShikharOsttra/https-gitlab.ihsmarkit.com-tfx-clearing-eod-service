package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;

import java.util.Comparator;
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
        return participantRepository.findAllCodes().stream().sorted(clhsLast());
    }

    private static Comparator<String> clhsLast() {
        return (code1, code2) -> {
            if (CLEARING_HOUSE_CODE.equals(code1)) {
                return CLEARING_HOUSE_CODE.equals(code2) ? 0 : 1;
            } else {
                return CLEARING_HOUSE_CODE.equals(code2) ? -1 : code1.compareTo(code2);
            }
        };
    }

}
