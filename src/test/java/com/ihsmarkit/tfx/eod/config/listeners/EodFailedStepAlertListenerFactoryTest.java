package com.ihsmarkit.tfx.eod.config.listeners;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import com.ihsmarkit.tfx.alert.client.domain.EodMtmFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodNettingFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceCsvGenerationFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceSendingEmailFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.NewAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.exception.RebalancingCsvGenerationException;
import com.ihsmarkit.tfx.eod.exception.RebalancingMailSendingException;

import lombok.RequiredArgsConstructor;

@ExtendWith(MockitoExtension.class)
class EodFailedStepAlertListenerFactoryTest {

    private static final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.now();

    @Mock
    private ClockService clockService;
    @Mock
    private AlertSender alertSender;
    @InjectMocks
    private EodFailedStepAlertListenerFactory eodFailedStepAlertSender;

    @Test
    void shouldSendAlert() {
        when(clockService.getCurrentDateTimeUTC()).thenReturn(CURRENT_DATE_TIME);

        final Map<TestSetup, NewAlert> testerMap = Map.of(
            TestSetup.of(eodFailedStepAlertSender.mtmFailedListener(), new RuntimeException()),
            EodMtmFailedAlert.of(CURRENT_DATE_TIME),

            TestSetup.of(eodFailedStepAlertSender.nettingFailedListener(), new RuntimeException()),
            EodNettingFailedAlert.of(CURRENT_DATE_TIME),

            TestSetup.of(eodFailedStepAlertSender.rebalancingProcessFailedListener(), new RuntimeException()),
            EodPositionRebalanceFailedAlert.of(CURRENT_DATE_TIME),

            TestSetup.of(eodFailedStepAlertSender.rebalancingProcessFailedListener(), new RebalancingCsvGenerationException(new RuntimeException())),
            EodPositionRebalanceCsvGenerationFailedAlert.of(CURRENT_DATE_TIME),

            TestSetup.of(eodFailedStepAlertSender.rebalancingProcessFailedListener(), new RebalancingMailSendingException(new RuntimeException())),
            EodPositionRebalanceSendingEmailFailedAlert.of(CURRENT_DATE_TIME)
        );

        testerMap.forEach((testSetup, expectedAlert) -> {
            final StepExecution stepExecution = new StepExecution("stepName", mock(JobExecution.class));
            stepExecution.addFailureException(testSetup.throwedException);

            testSetup.listener.afterStep(stepExecution);
            verify(alertSender).sendAlert(expectedAlert);
        });

        verifyNoMoreInteractions(alertSender);
    }

    @Test
    void shouldNotSendAlert_whenStepFinishedSuccessfully() {
        List.of(
            eodFailedStepAlertSender.mtmFailedListener(),
            eodFailedStepAlertSender.nettingFailedListener(),
            eodFailedStepAlertSender.rebalancingProcessFailedListener()
        )
            .forEach(listener -> {
                listener.afterStep(new StepExecution("stepName", mock(JobExecution.class)));
                verifyZeroInteractions(alertSender);
            });
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class TestSetup {
        private final StepExecutionListener listener;
        private final Throwable throwedException;
    }

}