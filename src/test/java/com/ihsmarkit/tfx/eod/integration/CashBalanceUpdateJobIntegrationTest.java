package com.ihsmarkit.tfx.eod.integration;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.CASH_BALANCE_UPDATE_BATCH_JOB_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.eod.config.CashCollateralBalanceUpdateJobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    CashCollateralBalanceUpdateJobConfig.class
})
@TestPropertySource("classpath:/application.properties")
public class CashBalanceUpdateJobIntegrationTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2019, 10, 8, 16, 0);

    @Autowired
    @Qualifier(value = CASH_BALANCE_UPDATE_BATCH_JOB_NAME)
    private Job job;

    @Autowired
    private JobLauncher jobLauncher;

    @Test
    @DatabaseSetup({"/common/currency.xml", "/common/participants.xml", "/common/issuerBanks.xml", "/cash-balance-update/cash-balance-update-20191007.xml"})
    @ExpectedDatabase(value = "/cash-balance-update/cash-balance-update-20191007-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testBalanceUpdate() throws Exception {

        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191008").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(job, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
