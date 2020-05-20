package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.COLLATERAL_LIST_LEDGER_STEP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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
class CollateralListLedgerStepTest extends AbstractSpringBatchTest {

    @MockBean
    private ClockService clockService;

    @Test
    @DatabaseSetup({
        "/common/business_date_2019_10_07.xml",
        "/common/issuerBanks.xml",
        "/common/haircuts.xml",
        "/common/currency.xml",
        "/common/calendar.xml",
        "/common/participants.xml",
        "/eod2Job/CollateralListLedger_setup.xml"
    })
    @ExpectedDatabase(value = "/eod2Job/CollateralListLedger_expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testCollateralListLedgerStep() {
        when(clockService.getCurrentDateTime()).thenReturn(LocalDateTime.of(2019, 10, 8, 11, 30, 0, 0));

        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007")
            .toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.launchStep(COLLATERAL_LIST_LEDGER_STEP_NAME, jobParams);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

}
