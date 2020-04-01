package com.ihsmarkit.tfx.eod.config.listeners;

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

    public void mtmFailed(final Throwable cause) {
        log.info("Send MtM failed alert", cause);
        alertSender.sendAlert(EodMtmFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
    }

    public void nettingFailed(final Throwable cause) {
        log.info("Send Netting failed alert", cause);
        alertSender.sendAlert(EodNettingFailedAlert.of(clockService.getCurrentDateTimeUTC(), cause.getMessage()));
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

}
