package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.CASH;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;

import java.time.LocalDate;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodCashSettlementEntity;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodCashSettlementRepository;
import com.ihsmarkit.tfx.core.domain.notification.system.CollateralBalanceChangeEventNotification;
import com.ihsmarkit.tfx.core.domain.notification.system.SystemEventNotificationSender;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class CollateralBalanceUpdateNotifierTasklet implements Tasklet {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final SystemEventNotificationSender systemEventNotificationSender;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    private final EodCashSettlementRepository eodCashSettlementRepository;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final List<EodCashSettlementEntity> margins = getEodCashSettlementEntities();

        margins.stream()
            .map(entity -> entity.getParticipant().getCode())
            .forEach(this::publishCollateralUpdateNotification);

        return RepeatStatus.FINISHED;
    }

    private List<EodCashSettlementEntity> getEodCashSettlementEntities() {
        final LocalDate previousTradingDate = calendarTradingSwapPointRepository.findPreviousTradingDateFailFast(businessDate);
        return eodCashSettlementRepository.findAllActionableCashSettlements(previousTradingDate);
    }

    private void publishCollateralUpdateNotification(final String code) {
        systemEventNotificationSender.send(CollateralBalanceChangeEventNotification.builder()
            .participantCode(code)
            .purpose(MARGIN)
            .productType(CASH)
            .build());
    }
}