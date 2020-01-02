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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ContextHierarchy({
    @ContextConfiguration(classes = IntegrationTestConfig.class),
    @ContextConfiguration(classes = EOD1JobConfig.class)}
)
@TestPropertySource("classpath:/application.properties")
class Eod1JobIntegrationTest {

    @Autowired
    @Qualifier(value = "eod1Job")
    private Job eodJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Test
    @DatabaseSetup({"/common/currency.xml", "/eod1Job/eod1-sunnyDay-20191007.xml"})
    @ExpectedDatabase(value = "/eod1Job/eod1-sunnyDay-20191007-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testEodJob() throws Exception {
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(eodJob, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
