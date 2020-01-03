package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.BUY;
import static com.ihsmarkit.tfx.core.domain.type.ParticipantPositionType.SELL;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class MonthlyTradingVolumeReader implements ItemReader<List<ParticipantPositionEntity>> {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;
    private final ParticipantPositionRepository participantPositionRepository;
    private boolean isFinished;

    @Override
    @SuppressFBWarnings("USFW_UNSYNCHRONIZED_SINGLETON_FIELD_WRITES")
    public List<ParticipantPositionEntity> read() {
        // workaround: https://stackoverflow.com/a/44278809/4760059
        if (isFinished) {
            return null;
        }
        isFinished = true;

        return participantPositionRepository.findAllByPositionTypeInAndTradeDateFetchCurrencyPairAndParticipant(Set.of(SELL, BUY), businessDate);
    }

}
