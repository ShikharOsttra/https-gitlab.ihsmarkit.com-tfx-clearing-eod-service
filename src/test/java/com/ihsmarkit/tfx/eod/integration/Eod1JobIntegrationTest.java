package com.ihsmarkit.tfx.eod.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.ihsmarkit.tfx.alert.client.domain.Eod1CompletedAlert;
import com.ihsmarkit.tfx.alert.client.domain.Eod1StartAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodStepCompleteAlert;
import com.ihsmarkit.tfx.alert.client.jms.AlertSender;
import com.ihsmarkit.tfx.common.test.assertion.Matchers;
import com.ihsmarkit.tfx.core.domain.transaction.NewTransaction;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.service.TransactionsSender;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;
import com.ihsmarkit.tfx.test.utils.db.DbUnitTestListeners;

@ExtendWith(SpringExtension.class)
@DbUnitTestListeners
@DatabaseTearDown("/common/tearDown.xml")
@ContextConfiguration(classes = {
    IntegrationTestConfig.class,
    EOD1JobConfig.class
})
@TestPropertySource("classpath:/application.properties")
class Eod1JobIntegrationTest {

    @Autowired
    @Qualifier(value = "eod1Job")
    private Job eodJob;

    @Autowired
    private JobLauncher jobLauncher;

    @MockBean
    private AwsSesMailClient mailClient;

    @MockBean
    private ClockService clockService;

    @MockBean
    private AlertSender alertSender;

    @Autowired
    private TransactionsSender transactionsSender;

    @Test
    @DatabaseSetup({ "/common/currency.xml", "/common/fx_spot_product.xml", "/common/participants.xml", "/eod1Job/eod1-sunnyDay-20191007.xml" })
    @ExpectedDatabase(value = "/eod1Job/eod1-sunnyDay-20191007-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testEodJob() throws Exception {
        final LocalDateTime currentDateTime = LocalDateTime.of(2019, 11, 12, 10, 10);
        final LocalDate businessDate = LocalDate.of(2019, 10, 7);
        when(clockService.getCurrentDateTimeUTC()).thenReturn(currentDateTime);

        final JobParameters jobParams = new JobParametersBuilder().addString("businessDate", "20191007").toJobParameters();
        final JobExecution jobExecution = jobLauncher.run(eodJob, jobParams);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        final InOrder inOrder = inOrder(alertSender);
        inOrder.verify(alertSender).sendAlert(Eod1StartAlert.of(currentDateTime, businessDate));
        inOrder.verify(alertSender, times(4)).sendAlert(any(EodStepCompleteAlert.class));
        inOrder.verify(alertSender).sendAlert(Eod1CompletedAlert.of(currentDateTime, businessDate));

        verifyTransactionsSend();
    }

    private void verifyTransactionsSend() {
        verify(transactionsSender).send(Matchers.argThat(request -> {
                assertThat(request.getTransactions()).hasSize(1);
                final NewTransaction newTransaction = request.getTransactions().get(0);
                assertThat(newTransaction.getBuyerParticipantId()).isEqualTo("P11");
                assertThat(newTransaction.getSellerParticipantId()).isEqualTo("P22");
                assertThat(newTransaction.getBaseCurrencyAmount().getValue()).isEqualByComparingTo(BigDecimal.valueOf(330000));
                assertThat(newTransaction.getSpotRate()).isEqualByComparingTo(BigDecimal.valueOf(1.1));
            }
        ));
    }
}
