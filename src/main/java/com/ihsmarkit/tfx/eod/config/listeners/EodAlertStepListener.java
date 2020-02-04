package com.ihsmarkit.tfx.eod.config.listeners;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.alert.client.domain.EodStepCompleteAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EodAlertStepListener extends StepExecutionListenerSupport {

    private final AlertSender alertSender;
    private final ClockService clockService;

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        // todo: get readable step name
        final EodStepCompleteAlert alert = EodStepCompleteAlert.of(clockService.getCurrentDateTimeUTC(), stepExecution.getStepName());
        alertSender.sendAlert(alert);

        log.debug("Sent alert about step complete: {}", alert);

        return null;
    }
}
