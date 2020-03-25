package com.ihsmarkit.tfx.eod.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import com.ihsmarkit.tfx.eod.config.EOD2JobConfig;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    EOD2JobConfig.class
})
@TestPropertySource("classpath:/application.properties")
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
class Eod2JobIntegrationTest {

    @Autowired
    @Qualifier(value = "eod2Job")
    private Job eodJob;

    @Autowired
    private JobLauncher jobLauncher;

//    @Autowired
//    DataSource ds;

    @Test
    @DatabaseSetup({
        "/common/currency.xml",
        "/common/participants.xml",
        "/common/business_date_2019_1_1.xml",
        "/common/issuerBanks.xml",
        "/common/haircuts.xml",
        "/common/fx_spot_product.xml",
        "/common/marginAlertConfiguration.xml",
        "/common/tradingHours.xml",
        "/eod1Job/eod2-sunnyDay-20191007.xml"
    })
    @ExpectedDatabase(value = "/eod1Job/eod2-sunnyDay-20191007-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testEodJob() throws Exception {
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(eodJob, jobParams);
//        DataSetExporter.getInstance().export(ds.getConnection(), new DataSetExportConfig()
//            .dataSetFormat(DataSetFormat.XML)
//            .outputFileName("target/eod2-expected.xml"));
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

}
