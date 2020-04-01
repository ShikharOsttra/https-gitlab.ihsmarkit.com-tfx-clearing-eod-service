package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.LEDGER_GENERATION_COMPLETED_ALERT_STEP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.ihsmarkit.tfx.alert.client.domain.EodLedgerGenerationCompletedAlert;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;

@Import(EOD2JobConfig.class)
class LedgerGenerationCompleterTaskletTest extends AbstractSpringBatchTest {

    @MockBean
    private ClockService clockService;

    @Test
    void shouldSendAlertAboutLedgerGenerationStepCompleted() {
        final LocalDateTime currentDateTime = LocalDateTime.now();

        when(clockService.getCurrentDateTimeUTC()).thenReturn(currentDateTime);

        final JobExecution execution = jobLauncherTestUtils.launchStep(LEDGER_GENERATION_COMPLETED_ALERT_STEP,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, LocalDate.now().format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        verify(alertSender).sendAlert(EodLedgerGenerationCompletedAlert.of(currentDateTime));
    }

}