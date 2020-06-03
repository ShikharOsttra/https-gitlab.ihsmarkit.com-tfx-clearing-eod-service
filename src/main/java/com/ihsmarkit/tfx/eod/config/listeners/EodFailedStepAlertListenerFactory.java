package com.ihsmarkit.tfx.eod.config.listeners;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.alert.client.domain.EodMtmFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodNettingFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceCsvGenerationFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceSendingEmailFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.MarginCashSettlementFailedAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.exception.RebalancingCsvGenerationException;
import com.ihsmarkit.tfx.eod.exception.RebalancingMailSendingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EodFailedStepAlertListenerFactory {

    private final AlertSender alertSender;
    private final ClockService clockService;

    public StepExecutionListener cashSettlementListener() {
        return new StepErrorListener() {
            @Override
            protected void onExceptionsAfterStep(final StepExecution stepExecution, final List<Throwable> exceptions) {
                log.info("Found {} exception(s) in cash settlement execution job", exceptions);
                alertSender.sendAlert(MarginCashSettlementFailedAlert.of(clockService.getCurrentDateTimeUTC(), getBusinessDateFromContext(stepExecution)));
            }

            private LocalDate getBusinessDateFromContext(final StepExecution stepExecution) {
                final String businessDate = stepExecution.getJobParameters().getString(BUSINESS_DATE_JOB_PARAM_NAME);
                return LocalDate.parse(businessDate, BUSINESS_DATE_FMT);
            }
        };
    }

    public StepExecutionListener mtmFailedListener() {
        return new StepErrorListener() {
            @Override
            protected void onExceptionsAfterStep(final StepExecution stepExecution, final List<Throwable> exceptions) {
                final Throwable cause = exceptions.get(0);
                log.info("Send MtM failed alert", cause);
                alertSender.sendAlert(EodMtmFailedAlert.of(clockService.getCurrentDateTimeUTC(), getCauseMessage(cause)));
            }
        };
    }

    public StepExecutionListener nettingFailedListener() {
        return new StepErrorListener() {
            @Override
            protected void onExceptionsAfterStep(final StepExecution stepExecution, final List<Throwable> exceptions) {
                final Throwable cause = exceptions.get(0);
                log.info("Send Netting failed alert", cause);
                alertSender.sendAlert(EodNettingFailedAlert.of(clockService.getCurrentDateTimeUTC(), getCauseMessage(cause)));
            }
        };
    }

    @SuppressWarnings({ "checkstyle:AnonInnerLength", "PMD.ConfusingTernary" })
    public StepExecutionListener rebalancingProcessFailedListener() {
        return new StepErrorListener() {
            @Override
            protected void onExceptionsAfterStep(final StepExecution stepExecution, final List<Throwable> exceptions) {
                @Nullable
                final Throwable csvFailException = findCauseByType(exceptions, RebalancingCsvGenerationException.class);
                @Nullable
                final Throwable mailSendingFailException = findCauseByType(exceptions, RebalancingMailSendingException.class);

                // rebalanced position, csv generation and email sending goes as a tasklet
                // only way to differ what has been failed is to handle separate exceptions
                if (csvFailException != null) {
                    log.info("Send Rebalancing CSV generation failed alert", csvFailException);
                    alertSender.sendAlert(EodPositionRebalanceCsvGenerationFailedAlert.of(
                        clockService.getCurrentDateTimeUTC(), getCauseMessage(csvFailException)
                    ));
                } else if (mailSendingFailException != null) {
                    log.info("Send Rebalancing mail sending failed alert", mailSendingFailException);
                    alertSender.sendAlert(EodPositionRebalanceSendingEmailFailedAlert.of(
                        clockService.getCurrentDateTimeUTC(), getCauseMessage(mailSendingFailException)
                    ));
                } else {
                    final Throwable cause = exceptions.get(0);
                    log.info("Send Rebalancing process failed alert", cause);
                    alertSender.sendAlert(EodPositionRebalanceFailedAlert.of(clockService.getCurrentDateTimeUTC(), getCauseMessage(cause)));
                }
            }
        };
    }

    private static String getCauseMessage(final Throwable cause) {
        return Objects.toString(
            cause.getMessage(),
            String.format("exception type: %s", cause.getClass().getCanonicalName())
        );
    }

}
