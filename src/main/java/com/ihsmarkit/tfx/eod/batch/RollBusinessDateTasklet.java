package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.eod.EodStage;
import com.ihsmarkit.tfx.core.domain.notification.system.EodEventNotification;
import com.ihsmarkit.tfx.core.domain.notification.system.SystemEventNotificationSender;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;
import com.ihsmarkit.tfx.eod.service.FutureValueService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
public class RollBusinessDateTasklet implements Tasklet {

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    private final SystemParameterRepository systemParameterRepository;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    private final FutureValueService futureValueService;

    private final SystemEventNotificationSender systemEventNotificationSender;

    private final CalendarDatesProvider calendarDatesProvider;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final LocalDate nextBusinessDate = calendarDatesProvider.getNextTradingDate(businessDate)
            .orElseThrow(() -> new IllegalStateException("Missing Trading/Swap calendar for given business date: " + businessDate));

        futureValueService.rollFutureValues(this.businessDate, nextBusinessDate);

        systemParameterRepository.setParameter(SystemParameters.BUSINESS_DATE, nextBusinessDate);

        systemEventNotificationSender.send(EodEventNotification.builder()
            .businessDate(businessDate)
            .eodStage(EodStage.ROLL_BUSINESS_DATE_COMPLETED)
            .build());

        return RepeatStatus.FINISHED;
    }

}
