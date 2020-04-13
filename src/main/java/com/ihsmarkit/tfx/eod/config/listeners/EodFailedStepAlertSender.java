package com.ihsmarkit.tfx.eod.config.listeners;

import java.util.List;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.alert.client.domain.EodMtmFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodNettingFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceCsvGenerationFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceSendingEmailFailedAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EodFailedStepAlertSender {

    private final AlertSender alertSender;
    private final ClockService clockService;

    public StepExecutionListener mtmFailedListener() {
        return new AlertSenderStepListener() {
            @Override
            protected void handleExceptions(final List<Throwable> exceptions) {
                final Throwable cause = exceptions.get(0);
                log.info("Send MtM failed alert", cause);
                alertSender.sendAlert(EodMtmFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
            }
        };
    }

    public StepExecutionListener nettingFailedListener() {
        return new AlertSenderStepListener() {
            @Override
            protected void handleExceptions(final List<Throwable> exceptions) {
                final Throwable cause = exceptions.get(0);
                log.info("Send Netting failed alert", cause);
                alertSender.sendAlert(EodNettingFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
            }
        };
    }

    public void rebalancingProcessFailed(final Throwable cause) {
        log.info("Send Rebalancing process failed alert", cause);
        alertSender.sendAlert(EodPositionRebalanceFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
    }

    public void rebalancingCsvFailed(final Throwable cause) {
        log.info("Send Rebalancing CSV generation failed alert", cause);
        alertSender.sendAlert(EodPositionRebalanceCsvGenerationFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
    }

    public void rebalancingEmailSendFailed(final Throwable cause) {
        log.info("Send Rebalancing mail sending failed alert", cause);
        alertSender.sendAlert(EodPositionRebalanceSendingEmailFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
    }

    private static abstract class AlertSenderStepListener extends StepExecutionListenerSupport {

        @Override
        public ExitStatus afterStep(final StepExecution stepExecution) {
            final List<Throwable> exceptions = stepExecution.getFailureExceptions();
            if (!exceptions.isEmpty()) {
                log.info("Step {} finished with {} error(s)", stepExecution.getStepName(), exceptions.size());
                handleExceptions(exceptions);
            }
            return null;
        }

        protected abstract void handleExceptions(List<Throwable> exceptions);
    }

}
