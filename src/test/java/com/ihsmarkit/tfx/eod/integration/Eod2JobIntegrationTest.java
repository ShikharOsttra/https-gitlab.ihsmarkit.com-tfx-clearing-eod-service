package com.ihsmarkit.tfx.eod.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import com.ihsmarkit.tfx.alert.client.domain.Eod2CompletedAlert;
import com.ihsmarkit.tfx.alert.client.domain.Eod2StartAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodStepCompleteAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.batch.ledger.monthlytradingvolume.LastTradingDateInMonthDecider;
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

    @MockBean
    private AlertSender alertSender;

    @MockBean
    private LastTradingDateInMonthDecider lastTradingDateInMonthDecider;

    @SpyBean
    private ClockService clockService;

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
        final LocalDateTime currentDateTime = LocalDateTime.of(2019, 11, 12, 10, 10);
        final LocalDate businessDate = LocalDate.of(2019, 10, 7);
        when(clockService.getCurrentDateTimeUTC()).thenReturn(currentDateTime);
        when(lastTradingDateInMonthDecider.decide(any(), any())).thenReturn(new FlowExecutionStatus(Boolean.TRUE.toString()));
        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(eodJob, jobParams);
//        DataSetExporter.getInstance().export(ds.getConnection(), new DataSetExportConfig()
//            .dataSetFormat(DataSetFormat.XML)
//            .outputFileName("target/eod2-expected.xml"));
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        final InOrder inOrder = inOrder(alertSender);
        inOrder.verify(alertSender).sendAlert(Eod2StartAlert.of(currentDateTime, businessDate));
        inOrder.verify(alertSender, times(11)).sendAlert(any(EodStepCompleteAlert.class));
        inOrder.verify(alertSender).sendAlert(Eod2CompletedAlert.of(currentDateTime, businessDate));
    }

}
