package com.ihsmarkit.tfx.eod.batch.ledger;

import static com.ihsmarkit.tfx.eod.batch.ledger.OrderUtils.buildIndexBasedOrder;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.data.util.Lazy;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class ParticipantCodeOrderIdProvider {

    private final ParticipantRepository participantRepository;

    private final Lazy<Map<String, Integer>> participantCodeOrderIdMap = Lazy.of(this::loadParticipantCodeOrderIdMap);

    public int get(final String participantCode) {
        return participantCodeOrderIdMap.get().getOrDefault(participantCode, 0);
    }

    private Map<String, Integer> loadParticipantCodeOrderIdMap() {
        final List<String> sortedCodes = participantRepository.findAll().stream()
            .map(ParticipantEntity::getCode)
            .sorted()
            .collect(toList());

        return buildIndexBasedOrder(sortedCodes);
    }

}
