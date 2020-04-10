package com.ihsmarkit.tfx.eod.config.listeners;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;

import java.time.LocalDate;
import java.util.List;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.alert.client.domain.MarginCashSettlementFailedAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashSettlementAlertSender {

    private final AlertSender alertSender;
    private final ClockService clockService;

    public StepExecutionListenerSupport listener() {
        return new StepExecutionListenerSupport() {
            @Override
            public ExitStatus afterStep(final StepExecution stepExecution) {
                final List<Throwable> exceptions = stepExecution.getFailureExceptions();
                if (!exceptions.isEmpty()) {
                    log.info("Found {} exception(s) in cash settlement execution job", exceptions);
                    alertSender.sendAlert(MarginCashSettlementFailedAlert.of(clockService.getCurrentDateTimeUTC(), getBusinessDateFromContext(stepExecution)));
                }

                return null;
            }

            private LocalDate getBusinessDateFromContext(final StepExecution stepExecution) {
                final String businessDate = stepExecution.getJobParameters().getString(BUSINESS_DATE_JOB_PARAM_NAME);
                return LocalDate.parse(businessDate, BUSINESS_DATE_FMT);
            }
        };
    }

}
