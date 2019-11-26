package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@JobScope
public class PositionRollTasklet implements Tasklet {

    private final ParticipantPositionRepository participantPositionRepository;

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {

        return RepeatStatus.FINISHED;
    }
}
