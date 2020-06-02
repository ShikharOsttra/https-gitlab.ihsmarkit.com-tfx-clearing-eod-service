package com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.GENERATE_MONTHLY_LEDGER_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ContextConfiguration(classes = EOD2JobConfig.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
class MonthlyTradingVolumeStepTest extends AbstractSpringBatchTest {

    @MockBean
    private ClockService clockService;

    @Test
    @DatabaseSetup({
        "/common/evaluation_date_2019_10_07.xml",
        "/eod2Job/MonthlyTradingVolumeStepTest_setup.xml"
    })
    @ExpectedDatabase(value = "/eod2Job/MonthlyTradingVolumeStepTest_expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRunMonthlyTradingVolumeStep() {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 2, 1, 11, 30, 0));

        final JobParameters jobParams = new JobParametersBuilder()
            .addString("businessDate", "20190131")
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchStep(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME, jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DatabaseSetup({
        "/common/evaluation_date_2019_10_07.xml",
        "/eod2Job/MonthlyTradingVolumeStepTest_lastTradingDayInMonth_setup.xml"
    })
    void shouldRunMonthlyTradingVolumeStepOnLastTradingDayInMonth() throws Exception {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 2, 1, 11, 30, 0));

        final JobParameters jobParams = new JobParametersBuilder()
            .addString("businessDate", "20190130")
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(jobExecution.getStepExecutions())
            .extracting(StepExecution::getStepName)
            .contains(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME);
    }

    @Test
    @DatabaseSetup({
        "/common/evaluation_date_2019_10_07.xml",
        "/eod2Job/MonthlyTradingVolumeStepTest_lastTradingDayInMonth_setup.xml"
    })
    void shouldNotRunMonthlyTradingVolumeStepOnNotLastTradingDayInMonth() throws Exception {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 2, 1, 11, 30, 0));

        final JobParameters jobParams = new JobParametersBuilder()
            .addString("businessDate", "20190129")
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(jobExecution.getStepExecutions())
            .extracting(StepExecution::getStepName)
            .doesNotContain(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME);
    }

    @Test
    @DatabaseSetup({
        "/common/evaluation_date_2019_10_07.xml",
        "/eod2Job/MonthlyTradingVolumeStepTest_lastTradingDayInMonth_setup.xml"
    })
    void shouldRunMonthlyTradingVolumeStep_OnNotLastTradingDayInMonth_AndGenerateMonthlyLedgerIsTrue() throws Exception {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 2, 1, 11, 30, 0));

        final JobParameters jobParams = new JobParametersBuilder()
            .addString("businessDate", "20190129")
            .addString(GENERATE_MONTHLY_LEDGER_JOB_PARAM_NAME, Boolean.TRUE.toString())
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(jobExecution.getStepExecutions())
            .extracting(StepExecution::getStepName)
            .contains(MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME);
    }

}