package com.ihsmarkit.tfx.eod.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.ihsmarkit.tfx.core.dl.config.CoreDlAutoConfiguration;
import com.ihsmarkit.tfx.eod.config.SpringBatchConfig;
import com.ihsmarkit.tfx.test.db.util.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(classes = CoreDlAutoConfiguration.class)
@Import(SpringBatchConfig.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@SpringBatchTest
class EodJobIntegrationTest {

    @Autowired
    @Qualifier(value = "eod1Job")
    private Job eodJob;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    @DatabaseSetup("/eod1Job/eod1-sunnyDay-20191007.xml")
    void testEodJob() throws Exception {
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007").toJobParameters();
        final JobExecution jobExecution = jobLauncherTestUtils.getJobLauncher().run(eodJob, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
