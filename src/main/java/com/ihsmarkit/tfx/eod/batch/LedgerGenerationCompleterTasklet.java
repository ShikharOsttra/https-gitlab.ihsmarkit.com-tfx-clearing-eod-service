package com.ihsmarkit.tfx.eod.batch;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.alert.client.domain.EodLedgerGenerationCompletedAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@JobScope
public class LedgerGenerationCompleterTasklet implements Tasklet {

    private final AlertSender alertSender;
    private final ClockService clockService;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final EodLedgerGenerationCompletedAlert alert = EodLedgerGenerationCompletedAlert.of(clockService.getCurrentDateTimeUTC());
        log.info("Send Ledger generation complete alert {}", alert);
        alertSender.sendAlert(alert);

        return RepeatStatus.FINISHED;
    }
}
