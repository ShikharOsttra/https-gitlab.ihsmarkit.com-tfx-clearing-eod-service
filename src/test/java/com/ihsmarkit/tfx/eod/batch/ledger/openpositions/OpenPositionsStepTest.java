package com.ihsmarkit.tfx.eod.batch.ledger.openpositions;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.OPEN_POSITIONS_LEDGER_STEP_NAME;
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
import com.ihsmarkit.tfx.eod.config.CacheConfig;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@Import({EOD2JobConfig.class, CacheConfig.class})
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
public class OpenPositionsStepTest extends AbstractSpringBatchTest {
    @MockBean
    private ClockService clockService;

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/fx_spot_product.xml", "/common/participants.xml", "/eod2Job/OpenPositionsStepTest_setup.xml"})
    @ExpectedDatabase(value = "/eod2Job/OpenPositionsStepTest_expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRuOpenPositionsStep() throws Exception {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 2, 1, 11, 30, 0));

        final JobParameters jobParams = new JobParametersBuilder()
            .addString("businessDate", "20190131")
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchStep(OPEN_POSITIONS_LEDGER_STEP_NAME, jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
