package com.ihsmarkit.tfx.eod.config.listeners;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.BiFunction;

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
@SuppressWarnings("checkstyle:AnonInnerLength")
public class EodJobListenerFactory {

    private final AlertSender alertSender;
    private final ClockService clockService;

    public JobExecutionListener listener(
        final BiFunction<LocalDateTime, LocalDate, EodAlert> eodStartAlert,
        final BiFunction<LocalDateTime, LocalDate, EodAlert> oedCompleteAlert
    ) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(final JobExecution jobExecution) {
                final EodAlert eodAlert = eodStartAlert.apply(clockService.getCurrentDateTimeUTC(), getBusinessDateFromContext(jobExecution));
                alertSender.sendAlert(eodAlert);

                log.info("Sent EOD start alert: {}", eodAlert);
            }

            @Override
            public void afterJob(final JobExecution jobExecution) {
                final EodAlert eodAlert = oedCompleteAlert.apply(clockService.getCurrentDateTimeUTC(), getBusinessDateFromContext(jobExecution));
                alertSender.sendAlert(eodAlert);

                log.info("Sent EOD competed alert: {}", eodAlert);
            }

            private LocalDate getBusinessDateFromContext(final JobExecution jobExecution) {
                final String businessDate = jobExecution.getJobParameters().getString(BUSINESS_DATE_JOB_PARAM_NAME);
                return LocalDate.parse(businessDate, BUSINESS_DATE_FMT);
            }
        };
    }
}
