package com.ihsmarkit.tfx.eod.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.eod.config.RollBusinessDateJobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    RollBusinessDateJobConfig.class
})
@TestPropertySource("classpath:/application.properties")
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
class RollBusinessDateJobIntegrationTest {

    @Autowired
    @Qualifier(value = "rollBusinessDateJob")
    private Job rollBusinessDateJob;

    @Autowired
    private JobLauncher jobLauncher;

    @MockBean
    private AlertSender alertSender;

    @Test
    @DatabaseSetup("/rollBusinessDateJob/RollBusinessDate_setup.xml")
    @ExpectedDatabase(value = "/rollBusinessDateJob/RollBusinessDate_expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRollBusinessDate() throws Exception {
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20190101").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(rollBusinessDateJob, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DatabaseSetup("/rollBusinessDateJob/RollBusinessDate_weekend_setup.xml")
    @ExpectedDatabase(value = "/rollBusinessDateJob/RollBusinessDate_weekend_expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void shouldRollBusinessDateWeekend() throws Exception {
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20190104").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(rollBusinessDateJob, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

}
