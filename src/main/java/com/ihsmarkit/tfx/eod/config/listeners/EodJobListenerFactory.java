package com.ihsmarkit.tfx.eod.config.listeners;

import java.time.LocalDateTime;
import java.util.function.Function;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.alert.client.domain.EodAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EodJobListenerFactory {

    private final AlertSender alertSender;
    private final ClockService clockService;

    public JobExecutionListener listener(final Function<LocalDateTime, EodAlert> eodStartAlert, final Function<LocalDateTime, EodAlert> oedCompleteAlert) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(final JobExecution jobExecution) {
                final EodAlert eodAlert = eodStartAlert.apply(clockService.getCurrentDateTimeUTC());
                alertSender.sendAlert(eodAlert);

                log.info("Sent EOD start alert: {}", eodAlert);
            }

            @Override
            public void afterJob(final JobExecution jobExecution) {
                final EodAlert eodAlert = oedCompleteAlert.apply(clockService.getCurrentDateTimeUTC());
                alertSender.sendAlert(eodAlert);

                log.info("Sent EOD competed alert: {}", eodAlert);
            }
        };
    }
}
