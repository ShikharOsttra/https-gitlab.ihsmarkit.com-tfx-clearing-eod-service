package com.ihsmarkit.tfx.eod.batch;

import java.time.LocalDate;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.repository.SystemParameterRepository;
import com.ihsmarkit.tfx.core.domain.eod.EodStage;
import com.ihsmarkit.tfx.core.domain.notification.system.EodEventNotification;
import com.ihsmarkit.tfx.core.domain.notification.system.SystemEventNotificationSender;
import com.ihsmarkit.tfx.core.domain.type.SystemParameters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@JobScope
@Slf4j
public class EodCompleteNotifierTasklet implements Tasklet {

    private final SystemEventNotificationSender systemEventNotificationSender;

    private final SystemParameterRepository systemParameterRepository;

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        final LocalDate businessDate = systemParameterRepository.getParameterValueFailFast(SystemParameters.BUSINESS_DATE);

        systemEventNotificationSender.send(EodEventNotification.builder()
            .businessDate(businessDate)
            .eodStage(EodStage.ROLL_BUSINESS_DATE_COMPLETED)
            .build());

        return RepeatStatus.FINISHED;
    }
}
