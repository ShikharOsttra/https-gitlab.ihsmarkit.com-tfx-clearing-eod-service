package com.ihsmarkit.tfx.eod.config.listeners;

import java.util.List;

import javax.annotation.Nullable;

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
import com.ihsmarkit.tfx.eod.exception.RebalancingCsvGenerationException;
import com.ihsmarkit.tfx.eod.exception.RebalancingMailSendingException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    @SuppressWarnings({ "checkstyle:AnonInnerLength", "PMD.ConfusingTernary" })
    public StepExecutionListener rebalancingProcessFailedListener() {
        return new AlertSenderStepListener() {
            @Override
            protected void handleExceptions(final List<Throwable> exceptions) {
                @Nullable
                final Throwable csvFailException = findCauseByType(exceptions, RebalancingCsvGenerationException.class);
                @Nullable
                final Throwable mailSendingFailException = findCauseByType(exceptions, RebalancingMailSendingException.class);

                if (csvFailException != null) {
                    log.info("Send Rebalancing CSV generation failed alert", csvFailException);
                    alertSender.sendAlert(EodPositionRebalanceCsvGenerationFailedAlert.of(clockService.getCurrentDateTimeUTC(), csvFailException.getMessage()));
                } else if (mailSendingFailException != null) {
                    log.info("Send Rebalancing mail sending failed alert", mailSendingFailException);
                    alertSender.sendAlert(EodPositionRebalanceSendingEmailFailedAlert.of(
                        clockService.getCurrentDateTimeUTC(), mailSendingFailException.getMessage()
                    ));
                } else {
                    final Throwable cause = exceptions.get(0);
                    log.info("Send Rebalancing process failed alert", cause);
                    alertSender.sendAlert(EodPositionRebalanceFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
                }
            }
        };
    }

    @Nullable
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private static Throwable findCauseByType(final List<Throwable> stepExceptions, final Class<? extends Throwable> wantedException) {
        return stepExceptions.stream()
            .filter(wantedException::isInstance)
            .findFirst()
            .map(Throwable::getCause)
            .orElse(null);
    }

    private abstract static class AlertSenderStepListener extends StepExecutionListenerSupport {

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
