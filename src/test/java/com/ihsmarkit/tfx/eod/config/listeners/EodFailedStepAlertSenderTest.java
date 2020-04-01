package com.ihsmarkit.tfx.eod.config.listeners;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.alert.client.domain.EodMtmFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodNettingFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceCsvGenerationFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceSendingEmailFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.NewAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;

@ExtendWith(MockitoExtension.class)
class EodFailedStepAlertSenderTest {

    @Mock
    private ClockService clockService;
    @Mock
    private AlertSender alertSender;
    @InjectMocks
    private EodFailedStepAlertSender eodFailedStepAlertSender;

    @Test
    void shouldSendAlert() {
        final String errorMessage = "errorMessage";
        final LocalDateTime currentDateTime = LocalDateTime.now();

        when(clockService.getCurrentDateTimeUTC()).thenReturn(currentDateTime);

        final Map<Consumer<Throwable>, NewAlert> testerMap = Map.of(
            eodFailedStepAlertSender::mtmFailed, EodMtmFailedAlert.of(currentDateTime, errorMessage),
            eodFailedStepAlertSender::nettingFailed, EodNettingFailedAlert.of(currentDateTime, errorMessage),
            eodFailedStepAlertSender::rebalancingProcessFailed, EodPositionRebalanceFailedAlert.of(currentDateTime, errorMessage),
            eodFailedStepAlertSender::rebalancingCsvFailed, EodPositionRebalanceCsvGenerationFailedAlert.of(currentDateTime, errorMessage),
            eodFailedStepAlertSender::rebalancingEmailSendFailed, EodPositionRebalanceSendingEmailFailedAlert.of(currentDateTime, errorMessage)
        );

        testerMap.forEach((consumer, expectedAlert) -> {
            consumer.accept(new Exception(errorMessage));
            verify(alertSender).sendAlert(expectedAlert);
        });

        verifyNoMoreInteractions(alertSender);
    }

}