package com.ihsmarkit.tfx.eod.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.test.db.util.DbUnitTestListeners;

@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
class EodJobIntegrationTest extends AbstractSpringBatchTest {

    @Autowired
    @Qualifier(value = "eod1Job")
    private Job eodJob;

    @Test
    @DatabaseSetup("/eod1Job/eod1-sunnyDay-20191007.xml")
    @ExpectedDatabase(value = "/eod1Job/eod1-sunnyDay-20191007-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void testEodJob() throws Exception {
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007").toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.getJobLauncher().run(eodJob, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
