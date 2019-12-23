package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.DAILY_MARKET_DATA_LEDGER_STEP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@Import(EOD2JobConfig.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
public class DailyMarketDataLedgerStepTest extends AbstractSpringBatchTest {

    @MockBean
    private ClockService clockService;

    @Test
    @DatabaseSetup("/eod2Job/DailyMarketDataLedger_setup.xml")
    @ExpectedDatabase(value = "/eod2Job/DailyMarketDataLedger_expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void shouldRunDailyMarketDataStep() {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 2, 2, 11, 30, 0));

        final JobParameters jobParams = new JobParametersBuilder()
            .addString("businessDate", "20190202")
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchStep(DAILY_MARKET_DATA_LEDGER_STEP_NAME, jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

}
