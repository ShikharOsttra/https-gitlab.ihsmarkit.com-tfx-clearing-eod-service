package com.ihsmarkit.tfx.eod.batch;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aLegalEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_JOB_PARAM_NAME;
import static com.ihsmarkit.tfx.eod.config.EodJobConstants.REBALANCE_PUBLISHING_STEP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceCsvGenerationFailedAlert;
import com.ihsmarkit.tfx.alert.client.domain.EodPositionRebalanceSendingEmailFailedAlert;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantStatus;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.config.AbstractSpringBatchTest;
import com.ihsmarkit.tfx.eod.config.EOD1JobConfig;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;

@ContextConfiguration(classes = EOD1JobConfig.class)
class RebalancePublishingTaskletTest extends AbstractSpringBatchTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 10, 6);

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private AwsSesMailClient mailClient;

    @MockBean
    private PositionRebalanceCSVWriter csvWriter;

    @MockBean
    private ClockService clockService;

    @MockBean
    private ParticipantRepository participantRepository;

    @Test
    void shouldSendAlert_whenCsvGenerationFailed() {
        when(tradeRepository.findAllBalanceByTradeDate(any()))
            .thenReturn(List.of(
                TradeEntity.builder()
                    .currencyPair(CurrencyPairEntity.of(1L, "USD", "EUR"))
                    .originator(aLegalEntityBuilder().build())
                    .counterparty(aLegalEntityBuilder().build())
                    .direction(Side.BUY)
                    .spotRate(BigDecimal.ONE)
                    .baseAmount(AmountEntity.of(BigDecimal.TEN, "USD"))
                    .valueAmount(AmountEntity.of(BigDecimal.TEN, "EUR"))
                    .tradeDate(LocalDate.now())
                    .valueDate(LocalDate.now())
                    .submissionTsp(LocalDateTime.now())
                    .build()
            ));

        when(csvWriter.getRecordsAsCsv(anyList()))
            .thenThrow(new RuntimeException());
        final LocalDateTime alertTime = LocalDateTime.now();
        when(clockService.getCurrentDateTimeUTC()).thenReturn(alertTime);

        final JobExecution execution = jobLauncherTestUtils.launchStep(REBALANCE_PUBLISHING_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);

        verify(alertSender).sendAlert(EodPositionRebalanceCsvGenerationFailedAlert.of(alertTime));
        verifyNoMoreInteractions(alertSender);
    }

    @Test
    void shouldSendAlert_whenMailSendingFailed() {
        when(csvWriter.getRecordsAsCsv(anyList())).thenReturn("csv");
        when(tradeRepository.findAllBalanceByTradeDate(any()))
            .thenReturn(List.of(
                TradeEntity.builder()
                    .currencyPair(CurrencyPairEntity.of(1L, "USD", "EUR"))
                    .originator(aLegalEntityBuilder().build())
                    .counterparty(aLegalEntityBuilder().build())
                    .direction(Side.BUY)
                    .spotRate(BigDecimal.ONE)
                    .baseAmount(AmountEntity.of(BigDecimal.TEN, "USD"))
                    .valueAmount(AmountEntity.of(BigDecimal.TEN, "EUR"))
                    .tradeDate(LocalDate.now())
                    .valueDate(LocalDate.now())
                    .submissionTsp(LocalDateTime.now())
                    .build()
            ));

        doThrow(new RuntimeException()).when(mailClient).sendEmail(any());
        final LocalDateTime alertTime = LocalDateTime.now();
        when(clockService.getCurrentDateTimeUTC()).thenReturn(alertTime);
        when(participantRepository.findAllNotDeletedParticipantListItems())
            .thenReturn(List.of(
                aParticipantEntityBuilder().type(ParticipantType.LIQUIDITY_PROVIDER).status(ParticipantStatus.ACTIVE).build()
            ));

        final JobExecution execution = jobLauncherTestUtils.launchStep(REBALANCE_PUBLISHING_STEP_NAME,
            new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString(BUSINESS_DATE_JOB_PARAM_NAME, BUSINESS_DATE.format(BUSINESS_DATE_FMT))
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);

        verify(alertSender).sendAlert(EodPositionRebalanceSendingEmailFailedAlert.of(alertTime));
        verifyNoMoreInteractions(alertSender);
    }

}