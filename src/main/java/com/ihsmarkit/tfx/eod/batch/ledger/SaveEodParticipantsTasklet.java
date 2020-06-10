package com.ihsmarkit.tfx.eod.batch.ledger;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodParticipantEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodParticipantRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class SaveEodParticipantsTasklet implements Tasklet {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final ParticipantRepository participantRepository;

    private final EodParticipantRepository eodParticipantRepository;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {

        participantRepository.findAll(SpecificationFactory.participantPathSpecification().toRootSpecification())
            .stream()
            .map(participant -> EodParticipantEntity.builder()
                .participant(participant)
                .date(businessDate)
                .build()
            ).forEach(eodParticipantRepository::save);

        return RepeatStatus.FINISHED;
    }
}
