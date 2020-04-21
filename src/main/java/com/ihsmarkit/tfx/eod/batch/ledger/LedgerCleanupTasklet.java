package com.ihsmarkit.tfx.eod.batch.ledger;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.eod.service.EODCleanupService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class LedgerCleanupTasklet implements Tasklet {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final EODCleanupService eodCleanupService;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        log.info("Cleaning up ledgers for {}.", businessDate);
        eodCleanupService.deleteAllLedgersByBusinessDate(businessDate);
        return RepeatStatus.FINISHED;
    }
}
